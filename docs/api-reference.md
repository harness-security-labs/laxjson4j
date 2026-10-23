# API Reference

Complete reference for the public surface of laxjson4j: the `Json` facade, the `JsonNode` AST, and the two exception types.

- [Json — Entry Point](#json--entry-point)
  - [Parsing](#parsing)
  - [Serialization](#serialization)
  - [Flattening](#flattening)
  - [Object Conversion](#object-conversion)
- [JsonNode — The AST](#jsonnode--the-ast)
  - [NodeType Enum](#nodetype-enum)
  - [Base Class Methods](#base-class-methods)
  - [ObjectNode](#objectnode)
  - [ArrayNode](#arraynode)
  - [StringNode](#stringnode)
  - [Number Nodes](#number-nodes)
  - [BooleanNode](#booleannode)
  - [NullNode](#nullnode)
- [Exceptions](#exceptions)
  - [JsonParseException](#jsonparseexception)
  - [JsonConversionException](#jsonconversionexception)

---

## Json — Entry Point

**Package:** `ai.traceable.laxjson4j`

The `Json` class is the main facade. Create one instance and reuse it across threads — it maintains internal pooled parsers and a shared field-name table that improve performance with repeated use.

```java
Json json = new Json();
```

### Parsing

| Method                        | Description                                                                                                                                                                                       |
|-------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `JsonNode parse(String json)` | Parses a JSON string into a `JsonNode` tree. The root is always an `ObjectNode` or `ArrayNode`. Throws `JsonParseException` for unparseable input, `IllegalArgumentException` if input is `null`. |

### Serialization

| Method                                                        | Description                                                                                                  |
|---------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| `String stringify(JsonNode node)`                      | Serializes a node tree into compact JSON (no extra whitespace).                                              |
| `String stringify(JsonNode node, boolean prettyPrint)` | Serializes a node tree. If `prettyPrint` is `true`, produces human-readable output with 2-space indentation. |
| `String prettyPrint(JsonNode node)`                    | Serializes a node tree with 2-space indentation and newlines.                                                |
| `String fixJsonString(String json)`                           | Parses malformed JSON and re-serializes it into valid compact JSON. Equivalent to `parse(json).toString()`.  |
| `String fixJsonString(String json, boolean prettyPrint)`      | Parses malformed JSON and re-serializes it, optionally pretty-printed.                                       |

### Flattening

| Method                                                                 | Description                                                                                                           |
|------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| `Map<String, String> flattenJSON(String json)`                  | Parses JSON and flattens into a map of dot-separated paths to string values. Array indices are attached directly to the preceding path segment as `[i]` (e.g. `a.b[0]`). |
| `Map<String, String> flattenJSON(String json, String prefix)`   | Same as above, with a prefix prepended to all keys.                                                                   |
| `Map<String, String> flattenJSON(JsonNode node)`                | Flattens a node tree into a dot-path map.                                                                             |
| `Map<String, String> flattenJSON(JsonNode node, String prefix)` | Flattens a node tree with a key prefix.                                                                               |

### Object Conversion

| Method                                                 | Description                                                                                                                                                                                                                                                |
|--------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `<T> T toObject(JsonNode node, Class<T> clazz)` | Converts a `JsonNode` tree into a Java object. Supports primitives, boxed types, `String`, `BigDecimal`, `BigInteger`, enums, `List`, `Set`, `Map`, arrays, POJOs, and date types (`Date`, `Timestamp`, `Instant`, `LocalDate`, `LocalDateTime`). |
| `<T> T fromJson(String json, Class<T> clazz)`          | Parses JSON and converts directly to a Java object. Convenience for `toObject(parse(json), clazz)`.                                                                                                                                                        |

---

## JsonNode — The AST

**Package:** `ai.traceable.laxjson4j.node`

`JsonNode` is an abstract base class with 10 concrete subtypes. It implements `Iterable<JsonNode>`, so any node can be used directly in an enhanced-for loop (see [Container accessors](#base-class-methods)).

```
JsonNode (abstract)
├── ObjectNode       — key-value pairs
├── ArrayNode        — ordered list of nodes
├── StringNode       — text value
├── IntNode          — int-range integers
├── LongNode         — long-range integers
├── DoubleNode       — double-precision floats
├── BigIntegerNode   — arbitrary-precision integers
├── BigDecimalNode   — arbitrary-precision decimals
├── BooleanNode      — true / false
└── NullNode         — null
```

### NodeType Enum

Each node has a `type()` method returning one of:

`OBJECT`, `ARRAY`, `STRING`, `INT`, `LONG`, `DOUBLE`, `BIG_INTEGER`, `BIG_DECIMAL`, `BOOLEAN`, `NULL`

### Base Class Methods

These methods are available on all `JsonNode` instances. Subclasses override them to return meaningful values; the base class returns safe defaults (0, `false`, `null`).

**Type checks:**

| Method                | Returns `true` for                                                      |
|-----------------------|-------------------------------------------------------------------------|
| `boolean isObject()`  | `ObjectNode`                                                            |
| `boolean isArray()`   | `ArrayNode`                                                             |
| `boolean isNull()`    | `NullNode`                                                              |
| `boolean isNumber()`  | `IntNode`, `LongNode`, `DoubleNode`, `BigIntegerNode`, `BigDecimalNode` |
| `boolean isTextual()` | `StringNode`                                                            |
| `boolean isBoolean()` | `BooleanNode`                                                           |
| `boolean isValueNode()` | Any scalar — string, number, boolean, or null (i.e. not an object or array). |
| `boolean isEmpty()`   | Empty containers (`{}`, `[]`) and all scalars/null (they report `size() == 0`). |

**Value accessors (safe defaults):**

| Method                      | Default | Override behavior                                                                                                                                              |
|-----------------------------|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `String asText()`           | `null`  | Returns the string representation of the value. Number nodes return their numeric text, `BooleanNode` returns `"true"`/`"false"`, `NullNode` returns `"null"`. |
| `int asInt()`               | `0`     | Number nodes return their value cast/converted to `int`. `BooleanNode` returns `1` or `0`.                                                                     |
| `long asLong()`             | `0L`    | Number nodes return their value as `long`.                                                                                                                     |
| `double asDouble()`         | `0.0`   | Number nodes return their value as `double`.                                                                                                                   |
| `BigInteger asBigInt()`     | `null`  | `IntNode`, `LongNode`, `BigIntegerNode` return their value as `BigInteger`. `BigDecimalNode` delegates to `asBigDecimal().toBigInteger()` indirectly.          |
| `BigDecimal asBigDecimal()` | `null`  | `BigDecimalNode` lazily parses and returns the value. `BigIntegerNode` wraps its value.                                                                        |
| `Number asNumber()`         | `null`  | Returns the most specific `Number` type for numeric nodes.                                                                                                     |
| `boolean asBoolean()`       | `false` | `BooleanNode` returns its value. Number nodes return `true` if non-zero. `StringNode` returns `true` if the text is `"true"` (case-insensitive).               |

**Container accessors:**

| Method                                           | Default        | Override behavior                                                                    |
|--------------------------------------------------|----------------|--------------------------------------------------------------------------------------|
| `JsonNode get(String key)`                       | `null`         | `ObjectNode` returns the value for the given key, or `null` if absent.               |
| `JsonNode get(int index)`                        | `null`         | `ArrayNode` returns the element at the index, or `null` if out of bounds.            |
| `int size()`                                     | `0`            | `ObjectNode` returns the number of keys. `ArrayNode` returns the number of elements. |
| `Iterator<JsonNode> elements()`                  | Empty iterator | `ArrayNode` returns an iterator over its elements; `ObjectNode` returns an iterator over its field **values** (insertion order). |
| `Iterator<JsonNode> iterator()`                  | Delegates to `elements()` | Lets any node be used in an enhanced-for loop. Arrays yield elements, objects yield field values, scalars/null iterate zero times (Jackson parity). |
| `Iterator<Map.Entry<String, JsonNode>> fields()` | Empty iterator | `ObjectNode` returns an iterator over its key-value entries.                         |

**Serialization:**

| Method                                 | Description                                                                                                    |
|----------------------------------------|----------------------------------------------------------------------------------------------------------------|
| `String toString()`                    | Returns a compact JSON representation with this node as the root.                                              |
| `void writeTo(StringBuilder sb)`       | Appends the compact JSON representation of this node directly to `sb`. Avoids allocating an intermediate `String`; use this inside any code that builds up a larger output. |
| `static String escapeString(String s)` | Escapes a string for JSON output (handles `"`, `\`, control characters). Used internally during serialization. |

---

### ObjectNode

Represents a JSON object. Maintains insertion order. Objects with ≤8 fields use parallel flat arrays (`String[]`/`JsonNode[]`) for cache-efficient access; larger objects promote to `LinkedHashMap`.

| Method                                           | Description                                                                                                                                                |
|--------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `void put(String key, JsonNode value)`           | Adds or updates a key-value pair. If the key already exists, the values are merged into an `ArrayNode` (see [Duplicate Keys](usage.md#duplicate-keys)).    |
| `JsonNode get(String key)`                       | Returns the value for the key, or `null` if absent.                                                                                                        |
| `int size()`                                     | Returns the number of keys.                                                                                                                                |
| `Set<String> keySet()`                           | Returns the set of all keys in insertion order.                                                                                                            |
| `Iterator<Map.Entry<String, JsonNode>> fields()` | Returns an iterator over all key-value entries.                                                                                                            |
| `Iterator<JsonNode> elements()`                  | Returns an iterator over the field **values** in insertion order. Also drives the enhanced-for loop (`for (JsonNode v : objectNode)`).                     |
| `Stream<Map.Entry<String, JsonNode>> stream()`   | Returns a stream of key-value entries.                                                                                                                     |

### ArrayNode

Represents an ordered JSON array.

| Method                          | Description                                                                                                   |
|---------------------------------|---------------------------------------------------------------------------------------------------------------|
| `void add(JsonNode value)`      | Appends a node to the array.                                                                                  |
| `JsonNode set(int index, JsonNode value)` | Replaces the element at `index` (which must already exist) and returns the previous element. Throws `IndexOutOfBoundsException` if the index is out of range and `NullPointerException` if `value` is `null` (use `NullNode.INSTANCE` for JSON null).|
| `JsonNode get(int index)`       | Returns the element at the given index, or `null` if the index is out of bounds (including negative indices). |
| `int size()`                    | Returns the number of elements.                                                                               |
| `Iterator<JsonNode> elements()` | Returns an iterator over the elements.                                                                        |
| `Stream<JsonNode> stream()`     | Returns a stream of elements.                                                                                 |

### StringNode

Represents a JSON string value.

| Construction          | `new StringNode(String value)`                                  |
|-----------------------|-----------------------------------------------------------------|
| `String asText()`     | Returns the raw string value.                                   |
| `boolean asBoolean()` | Returns `true` if the value equals `"true"` (case-insensitive). |

### Number Nodes

The parser selects the narrowest numeric type that fits the value:

| Node Type        | Range                                                                          | Construction                                         |
|------------------|--------------------------------------------------------------------------------|------------------------------------------------------|
| `IntNode`        | `-2^31` to `2^31 - 1` (1–9 digit integers, and 10-digit integers in int range) | `IntNode.valueOf(int)` (cached for -1 to 10)         |
| `LongNode`       | `-2^63` to `2^63 - 1` (10–19 digit integers)                                   | `new LongNode(long)`                                 |
| `BigIntegerNode` | Arbitrary precision integers (20+ digits, or 19-digit overflow)                | `new BigIntegerNode(BigInteger)`                     |
| `DoubleNode`     | IEEE 754 double (floating-point values up to 25 characters)                    | `new DoubleNode(double)`                             |
| `BigDecimalNode` | Arbitrary precision decimals (floating-point values longer than 25 characters) | `new BigDecimalNode(String rawText)` — lazily parsed |

All number nodes override `asInt()`, `asLong()`, `asDouble()`, `asNumber()`, `asText()`, and `asBoolean()` (non-zero → `true`).

### BooleanNode

Represents `true` or `false`. Uses singleton instances.

| Member                                  | Description                        |
|-----------------------------------------|------------------------------------|
| `BooleanNode.TRUE`                      | Singleton for `true`.              |
| `BooleanNode.FALSE`                     | Singleton for `false`.             |
| `static BooleanNode valueOf(boolean v)` | Returns the appropriate singleton. |
| `boolean asBoolean()`                   | Returns the boolean value.         |
| `int asInt()`                           | `1` for true, `0` for false.       |

### NullNode

Represents JSON `null`. Uses a singleton instance.

| Member              | Description                  |
|---------------------|------------------------------|
| `NullNode.INSTANCE` | The singleton null node.     |
| `boolean isNull()`  | Always returns `true`.       |
| `String asText()`   | Returns the string `"null"`. |

---

## Exceptions

**Package:** `ai.traceable.laxjson4j.exception`

### JsonParseException

Thrown when the parser encounters an unrecoverable structural error.

```java
public class JsonParseException extends RuntimeException {
    public JsonParseException(String message, int line, int column);
    public int getLine();
    public int getColumn();
}
```

The `message` automatically includes the location suffix `(line X, column Y)`.

See [Error Handling](usage.md#error-handling) for the catalogue of conditions that trigger this exception.

### JsonConversionException

Thrown when `toObject()` or `fromJson()` cannot map a node tree to the target Java type.

```java
public class JsonConversionException extends RuntimeException {
    public JsonConversionException(String message);
    public JsonConversionException(String message, Throwable cause);
}
```
