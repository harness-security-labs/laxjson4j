# laxjson4j — Lenient JSON Parser

A high-performance, fault-tolerant JSON parser for Java 11+ that gracefully handles malformed inputs. Instead of throwing errors on common JSON deviations — missing commas, unquoted strings, trailing commas, truncated input, comments — the parser absorbs them and produces a clean, navigable AST.

Built for scenarios where JSON is imperfect: log extraction, LLM outputs, user-submitted payloads, config files with comments, and any pipeline where strict parsing would reject valid-enough data.

**Key properties:**
- **Lenient by default** — parses what strict parsers reject.
- **`JsonNode` AST** — clean type hierarchy with 10 concrete node types.
- **Thread-safe** — a single `Json` instance is safe to share across threads.
- **Designed for high-throughput reuse** — `ThreadLocal` parser pool, shared field-name interning, and zero-intermediate-`String` serialisation keep the steady-state allocation cost near zero (see [`docs/architecture.md`](docs/architecture.md)).
- **Minimal API surface** — three exported packages: `ai.traceable.laxjson4j`, `ai.traceable.laxjson4j.node`, `ai.traceable.laxjson4j.exception`.

---

## Performance highlight

Multi-thread parse throughput on a 3.5 MB input (`Threads.MAX`):

| Parser            |   ops/sec | vs laxjson4j   |
|-------------------|----------:|---------------:|
| **laxjson4j**     | **1,601** | —              |
| Jackson 2.18.2    |       447 | 3.58× slower   |
| Fastjson2 2.0.56  |     1,751 | 1.09× faster   |
| Gson 2.10.1       |       215 | 7.45× slower   |

The `ThreadLocal` parser pool eliminates per-call allocations, so throughput stays stable as core count increases. Jackson's per-parse allocations create GC pressure at scale, collapsing it to roughly a third of laxjson4j's throughput.

See [`docs/performance.md`](docs/performance.md) for the full benchmark suite (single-thread latency, multi-thread throughput, and historical internal improvements), all reported in the same `N.NN× faster/slower` format.

---

## Install

<!-- Publishing has not happened yet; once it has, the snippet below will be uncommented.

**Gradle:**
```groovy
implementation("ai.traceable:laxjson4j:0.1.0")
```

**Maven:**
```xml
<dependency>
  <groupId>ai.traceable</groupId>
  <artifactId>laxjson4j</artifactId>
  <version>0.1.0</version>
</dependency>
```
-->

For now, build from source: `./gradlew build`.

## Quick start

```java
Json json = new Json();
JsonNode root = json.parse("{\"name\": \"Alice\", \"age\": 30}");

String name = root.get("name").asText();   // "Alice"
int age = root.get("age").asInt();          // 30
```

Fix and pretty-print malformed JSON in one call:

```java
String fixed = new Json().fixJsonString("""
    {
      name: John Doe          // unquoted key + value, missing comma
      "skills": ["Java",]      // trailing comma
    }
    """, true);
```

---

## What it accepts

The parser is a strict superset of standard JSON. It also tolerates:

- Missing commas between object entries and array elements
- Trailing commas
- Unquoted keys and unquoted string values
- Truncated input (unclosed objects, arrays, and strings auto-complete)
- Duplicate keys (values collapse into an `ArrayNode`)
- `//` line comments and `/* … */` block comments

Each behaviour is shown with a before/after example in [`docs/usage.md`](docs/usage.md#lenient-parsing-features).

## Documentation

- [Usage guide](docs/usage.md) — lenient-parsing behaviours, error handling, and a cookbook of seven worked examples (parse, navigate, fix, flatten, convert to POJOs, handle duplicate keys, handle truncation).
- [API reference](docs/api-reference.md) — `Json` facade, `JsonNode` hierarchy, exception types.
- [Performance & benchmarks](docs/performance.md) — full benchmark tables and the optimisations behind them.
- [Architecture & design](docs/architecture.md) — package layout, parse-flow walkthrough, and key design decisions (for contributors).

## Building and testing

```bash
./gradlew test                # run the full test suite
./gradlew :benchmarks:jmh     # run the JMH benchmarks
```

## Contributing

External contributions are accepted. A change can be merged only after the contributor signs the Contributor License Agreement. SECENG applies that agreement on GitHub when a pull request is opened.

## License

Licensed under the Apache License, Version 2.0. See [`LICENSE.md`](LICENSE.md). Copyright and attribution are in [`NOTICE.md`](NOTICE.md).
