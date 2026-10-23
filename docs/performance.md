# Performance & Benchmarks

All comparisons against other parsers use the **`N.NN× faster/slower`** convention, computed as the ratio of the competitor's metric against the laxjson4j baseline:

- For latency tables (lower µs/op is better), the ratio is `competitor / laxjson4j`.
- For throughput tables (higher ops/sec is better), the ratio is `laxjson4j / competitor`.

A value above `1.00×` always means the competitor is worse than laxjson4j on that metric; below `1.00×` means it is better.

---

## laxjson4j vs other parsers — single-thread parse, 97 KB input

| Parser            |   µs/op | vs laxjson4j   |
|-------------------|--------:|---------------:|
| **laxjson4j**     | **165** | —              |
| Jackson 2.18.2    |   182.7 | 1.11× slower   |
| Gson 2.10.1       |   208.5 | 1.26× slower   |
| Fastjson2 2.0.56  |    93.0 | 1.77× faster   |
| Hjson 3.1.0       |   578.5 | 3.51× slower   |

## Multi-thread parse throughput, 3.5 MB input — `Threads.MAX`

| Parser            |   ops/sec | vs laxjson4j   |
|-------------------|----------:|---------------:|
| **laxjson4j**     | **1,601** | —              |
| Jackson 2.18.2    |       447 | 3.58× slower   |
| Fastjson2 2.0.56  |     1,751 | 1.09× faster   |
| Gson 2.10.1       |       215 | 7.45× slower   |

The large-input multi-thread result is the key differentiator: laxjson4j's `ThreadLocal` parser pool eliminates per-call allocations, so throughput stays stable as core count increases. Jackson's per-parse allocations create GC pressure at scale, collapsing it to 447 ops/sec vs laxjson4j's 1,601 — a 3.58× gap.

---

## Internal improvements (97 KB input, before → after)

These numbers track the impact of recent optimisations against an earlier revision of laxjson4j (not against other parsers).

| Operation   |     Before |    After | Improvement   |
|-------------|-----------:|---------:|:-------------:|
| Parse       |   229.8 µs | 174.8 µs | **−24%**      |
| Stringify   |   267.8 µs |  97.5 µs | **−64%**      |
| PrettyPrint |  1607.9 µs | 134.0 µs | **−92%**      |
| Flatten     |  1083.1 µs | 190.7 µs | **−82%**      |

Key changes that produced these numbers:

- `writeTo(StringBuilder)` on all nodes — eliminates intermediate `String` allocations during serialisation.
- Flat `String[]` / `JsonNode[]` arrays in `ObjectNode` for objects with ≤8 fields — eliminates 45.7% of parse allocations on medium payloads by removing per-entry `Map.Entry` objects.
- A single shared `StringBuilder` in `prettyPrint`.
- `StringBuilder` prefix backtracking in `flattenHelper` — avoids rebuilding key prefixes per entry.
- Local-variable caching in the `parseQuotedString` inner loop (the same technique used by Gson's hot path).
- Raw `JsonNode[]` backing in `ArrayNode` — eliminates the `ArrayList` wrapper plus the `checkcast` on each element access.

See [`docs/architecture.md`](architecture.md) for the design context behind each of these changes.
