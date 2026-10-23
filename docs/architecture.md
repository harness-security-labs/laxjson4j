# Architecture & Design

This document is intended for developers contributing to the parser. It covers the package layout, the lifecycle of a parse call, the major design decisions, and the test suite.

- [Package Overview](#package-overview)
- [Parse Flow](#parse-flow)
- [Key Design Decisions](#key-design-decisions)
- [Testing](#testing)

---

## Package Overview

| Package                            | Role                                                                                                                                                                                       |
|------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ai.traceable.laxjson4j`           | Public facade. Contains `Json` — the only class users instantiate.                                                                                                                         |
| `ai.traceable.laxjson4j.node`      | The `JsonNode` AST hierarchy and `NodeType` enum. Pure data types with no parsing logic.                                                                                            |
| `ai.traceable.laxjson4j.exception` | `JsonParseException` and `JsonConversionException`.                                                                                                                                        |
| `ai.traceable.laxjson4j.parser`    | Core parser internals. `JsonParser` (single-pass scanner), `CharBuf` (growable char buffer), `NameTable` (field-name interning hash table), `NumberParser` (optimized numeric conversion). |
| `ai.traceable.laxjson4j.pool`      | `ParserPool` — recycles `JsonParser`, `CharBuf`, and input buffers via `ThreadLocal`. Not exported.                                                                                        |
| `ai.traceable.laxjson4j.convert`   | `NodeConverter` (tree-to-object conversion), `ClassInfo` (cached class metadata and field discovery), `TypeResolver` (generic type resolution).                                            |

## Parse Flow

The lifecycle of a `Json.parse(String)` call:

```
Json.parse(String json)
│
├─ 1. Acquire a ParserPool from ThreadLocal cache
│     (or create a new one if first call on this thread)
│
├─ 2. ParserPool provides a JsonParser instance
│     (with pre-allocated char[] input buffer, CharBuf text buffer,
│      and a child NameTable snapshotted from the root)
│
├─ 3. JsonParser.init(json)
│     ├─ Copies input string into internal char[] buffer (first 32K chunk)
│     ├─ Resets line/column tracking
│     └─ Resets CharBuf
│
├─ 4. NameTable.resetFromRoot()
│     └─ Refreshes child snapshot from root (one atomic read, zero allocations)
│
├─ 5. JsonParser.parse()
│     ├─ skipWhitespace() — skips spaces, tabs, newlines, // and /* */ comments
│     ├─ parseValue() — dispatches on first character:
│     │   ├─ '{' → parseObject()
│     │   │         ├─ Loop: read key (quoted → parseFieldName with NameTable interning,
│     │   │         │                   unquoted → parseUnquotedFieldName)
│     │   │         ├─ Expect ':'
│     │   │         ├─ Recursive parseValue() for the value
│     │   │         └─ ObjectNode.put(key, value)
│     │   ├─ '[' → parseArray()
│     │   │         ├─ Loop: recursive parseValue() for each element
│     │   │         └─ ArrayNode.add(value)
│     │   ├─ '"' → parseQuotedString() → StringNode
│     │   ├─ 't'/'f' → parseLiteral() → BooleanNode (or StringNode if not a valid literal)
│     │   ├─ 'n' → parseLiteral() → NullNode (or StringNode)
│     │   ├─ '-'/digit → parseNumber() → IntNode/LongNode/BigIntegerNode/DoubleNode/BigDecimalNode
│     │   └─ other → parseUnquotedString() → StringNode
│     └─ Validates root is ObjectNode or ArrayNode (else JsonParseException)
│
├─ 6. finally: NameTable.mergeBack()
│     └─ CAS-publishes any new field names from child back to root table
│
└─ 7. Release ParserPool back to ThreadLocal cache
      └─ trimIfOversized() — if text buffer grew past 64K, replace it to avoid memory retention
```

**Buffer chunking:** The parser uses a 32K char buffer. Inputs larger than 32K are processed in chunks via `loadMore()` and `compactAndLoad()`, which refill the buffer from the original string as the parser advances. This avoids allocating a copy of the entire input.

## Key Design Decisions

**NameTable — Open-addressing hash table for field-name interning**

JSON payloads in production often share the same field names across requests. The `NameTable` interns these names so that repeated parses produce the exact same `String` instances (identity equality), reducing allocations and improving map lookup performance.

The root/child pattern enables thread safety: each parser thread works with a private child snapshot (zero-contention reads), and new names are CAS-published back to the shared root at parse end. The table caps at 8,192 entries to bound memory.

**CharBuf — Growable char buffer for string assembly**

String values containing escape sequences or spanning buffer boundaries cannot be captured as a simple `new String(buf, start, len)`. The `CharBuf` accumulates these characters. It is recycled across parses via `ParserPool`, so the common case is zero allocations for string building. If a parse encounters an unusually large string (>64K), the buffer is replaced on release to prevent memory retention.

**NumberParser — Tiered numeric conversion**

Integer parsing uses an unrolled switch-based multiplier (no loops for ≤9 digits), graduating to a high/low split for 10-18 digits, and a full loop for exactly 19 digits with lexicographic overflow checking. Floating-point values delegate to `fastdoubleparser` (`JavaDoubleParser`) for near-native-speed double conversion. Values longer than 25 characters or with 20+ integer digits produce `BigDecimalNode` or `BigIntegerNode`.

**JsonNode hierarchy**

`JsonNode` is an abstract base class with a fixed set of concrete subtypes. The `NodeType` enum provides a discriminant for code that prefers enum-based dispatch over `instanceof`.

**writeTo(StringBuilder) — zero-intermediate-String serialisation**

Every `JsonNode` subtype implements `writeTo(StringBuilder sb)`, which appends its compact JSON form directly to the caller-supplied buffer. `stringify()` creates one `StringBuilder`, calls `root.writeTo(sb)`, and returns `sb.toString()`. This cuts stringify time by ~64% compared to the previous pattern of building Strings at each node level and concatenating them.

**ObjectNode flat-array storage**

`ObjectNode` stores the first 8 key-value pairs as parallel `String[]`/`JsonNode[]` arrays rather than a `LinkedHashMap`. This eliminates the `Map.Entry` allocation per field (45.7% of all parse allocations on medium payloads) and keeps small objects in a single contiguous cache line. Objects that exceed 8 fields spill into a `LinkedHashMap` as before.

**Parser pooling**

`Json` maintains a `ThreadLocal<ParserPool>`. Each pool owns a `JsonParser` with its pre-allocated input buffer, text buffer, and child `NameTable`. This means the steady-state cost of parsing is zero allocations for parser infrastructure — only the tree nodes themselves are allocated.

**Lenient parsing strategy**

The parser uses a single-pass character scanner with no separate tokenizer/lexer stage. When a character doesn't match any known production (not a quote, brace, bracket, digit, or literal start), it falls through to `parseUnquotedString()` which collects everything until a line break or structural terminator. This design makes leniency the default rather than an opt-in mode.

## Testing

Tests are organized under `src/test/java/ai/traceable/laxjson4j/`:

| Test Class                    | Scope                                                                                     |
|-------------------------------|-------------------------------------------------------------------------------------------|
| `JsonEntryPointTest`          | `Json.parse()` — invalid inputs, stringify, large inputs, reuse, concurrency, re-entrancy |
| `JsonAccessorTest`            | Tree navigation — object fields, array traversal, missing keys, out-of-bounds             |
| `JsonSerializationTest`       | `stringify`, `fixJsonString`, `prettyPrint`, duplicate keys in serialization              |
| `JsonFlattenTest`             | `flattenJSON` — nested objects, arrays, prefixes, edge cases                              |
| `JsonConversionTest`          | `toObject` / `fromJson` — POJOs, nested objects, null handling                            |
| `JsonPoolTest`                | Parser pool recycling, oversized buffer trimming                                          |
| `JsonNodeTest`                | Node type checks, accessor defaults, `escapeString`                                       |
| `JsonParserIntegrationTest`   | End-to-end parser edge cases across the full input spectrum                               |
| `JsonParserScannerTest`       | Low-level scanner behavior — strings, numbers, comments, buffer boundaries                |
| `NumberParserTest`            | Numeric parsing edge cases — overflow, large values, scientific notation                  |
| `NameTableTest`               | Field-name interning, hash collisions, concurrency safety                                 |
| `CharBufTest`                 | Buffer growth, append, reset, strip                                                       |
| `NodeConverterConversionTest` | Converter — scalars, enums, collections, maps, arrays, dates, generics                    |
| `TypeResolverTest`            | Generic type resolution                                                                   |
| `ClassInfoTest`               | Class metadata discovery, field/constructor selection                                     |

**Running tests:**
```bash
./gradlew test
```

**Running benchmarks:**
```bash
./gradlew :benchmarks:jmh
```
