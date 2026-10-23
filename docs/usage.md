# Usage Guide

How laxjson4j behaves on real inputs, what it accepts, what it rejects, and how to drive it from your own code.

- [Lenient Parsing Features](#lenient-parsing-features)
  - [Missing Commas](#missing-commas)
  - [Trailing Commas](#trailing-commas)
  - [Unquoted Strings](#unquoted-strings)
  - [Truncated JSON](#truncated-json)
  - [Duplicate Keys](#duplicate-keys)
  - [Comments](#comments)
- [Error Handling](#error-handling)
  - [Parse Errors](#parse-errors)
  - [Conversion Errors](#conversion-errors)
- [Examples](#examples)
  - [1. Parse and Navigate](#1-parse-and-navigate)
  - [2. Fix and Pretty-Print Malformed JSON](#2-fix-and-pretty-print-malformed-json)
  - [3. Flatten JSON to Dot-Notation Map](#3-flatten-json-to-dot-notation-map)
  - [4. Convert JSON to a POJO](#4-convert-json-to-a-pojo)
  - [5. Handle Duplicate Keys](#5-handle-duplicate-keys)
  - [6. Handle Truncated JSON](#6-handle-truncated-json)

---

## Lenient Parsing Features

The parser accepts a superset of standard JSON. Below are the deviations it tolerates, each with a before/after example.

### Missing Commas

Commas between object entries or array elements can be omitted. The parser infers boundaries from line breaks and structural characters.

**Input:**
```json
{
  "name": "Alice"
  "age": 30
  "active": true
}
```

**Parsed output (re-serialized):**
```json
{"name":"Alice","age":30,"active":true}
```

### Trailing Commas

Trailing commas after the last element in objects and arrays are silently ignored.

**Input:**
```json
{
  "tags": ["java", "python", "go",],
  "count": 3,
}
```

**Parsed output:**
```json
{"tags":["java","python","go"],"count":3}
```

### Unquoted Strings

Keys and values that appear without quotes are collected as strings. Values that happen to look like valid numbers, booleans, or null are still parsed as their native type — only truly unrecognizable tokens become `StringNode`s.

**Input:**
```json
{
  name: John Doe
  department: Engineering
  age: 30
}
```

**Parsed output:**
```json
{"name":"John Doe","department":"Engineering","age":30}
```

Note: `name` and `department` are unquoted keys; `John Doe` and `Engineering` are unquoted string values. `30` is parsed as an `IntNode`, not a string.

### Truncated JSON

When the input ends unexpectedly (unclosed braces, brackets, or strings), the parser auto-completes the structure rather than throwing an error.

**Input:**
```json
{
  "project": {
    "name": "Security Audit",
    "team": ["Alice", "Bob", "Charlie"
```

**Parsed output:**
```json
{"project":{"name":"Security Audit","team":["Alice","Bob","Charlie"]}}
```

### Duplicate Keys

When the same key appears multiple times in an object, all values are preserved by collapsing them into an `ArrayNode`.

**Input:**
```json
{
  "tag": "alpha",
  "tag": "beta",
  "tag": "gamma"
}
```

**Parsed output:**
```json
{"tag":["alpha","beta","gamma"]}
```

### Comments

Single-line (`//`) and block (`/* ... */`) comments are stripped during parsing.

**Input:**
```json
{
  // Database configuration
  "host": "localhost",
  "port": 5432,
  /*
    Credentials are loaded from env vars at runtime.
    These are placeholder defaults.
  */
  "user": "admin"
}
```

**Parsed output:**
```json
{"host":"localhost","port":5432,"user":"admin"}
```

---

## Error Handling

While the parser is lenient, some inputs are fundamentally unparseable. These produce exceptions with precise location information.

### Parse Errors

`JsonParseException` (extends `RuntimeException`) is thrown for structural errors that cannot be recovered from. Every instance carries `getLine()` and `getColumn()` for the error location.

| Error                                               | When                                                            | Example Input                   |
|-----------------------------------------------------|-----------------------------------------------------------------|---------------------------------|
| `JSON root must be an object or array`              | The top-level value is a scalar (string, number, boolean, null) | `"hello"`, `42`, `true`, `null` |
| `Object key must be a string, found: '...'`         | A structural token appears where an object key is expected      | `{: "value"}`, `{[ "value"]}`   |
| `Expected ':' after object key '...', found: '...'` | Missing colon between key and value                             | `{"key" "value"}`               |
| `Invalid hex digit in unicode escape: ...`          | A `\uXXXX` sequence contains a non-hex character                | `{"k": "\uZZZZ"}`               |

Additionally, `IllegalArgumentException` is thrown if `null` is passed to `Json.parse()`.

**Example — catching a parse error:**
```java
Json json = new Json();
try {
    json.parse("\"just a string\"");
} catch (JsonParseException e) {
    System.out.println(e.getMessage());  // JSON root must be an object or array (line 1, column 16)
    System.out.println(e.getLine());     // 1
    System.out.println(e.getColumn());   // 16
}
```

**Example — invalid object key:**
```java
try {
    json.parse("{: \"value\"}");
} catch (JsonParseException e) {
    // Object key must be a string, found: ':' (line 1, column 2)
}
```

**Example — missing colon:**
```java
try {
    json.parse("{\"key\" \"value\"}");
} catch (JsonParseException e) {
    // Expected ':' after object key 'key', found: '"' (line 1, column 7)
}
```

### Conversion Errors

`JsonConversionException` (extends `RuntimeException`) is thrown when `toObject()` or `fromJson()` cannot map a node tree to the requested Java type.

Common causes:
- Attempting to convert a non-`ObjectNode` into a POJO (e.g. an array where an object is expected)
- Invalid enum constant name
- Unsupported target date type
- Reflection failures (no accessible constructor, inaccessible fields)

```java
try {
    // Trying to convert an array node into a POJO
    json.fromJson("[1, 2, 3]", Person.class);
} catch (JsonConversionException e) {
    // Cannot convert ARRAY to Person: expected OBJECT node
}
```

---

## Examples

### 1. Parse and Navigate

```java
Json json = new Json();
JsonNode root = json.parse("""
    {
      "company": "TechCorp",
      "yearFounded": 2010,
      "isPublic": false,
      "headquarters": {
        "city": "San Francisco",
        "state": "CA"
      },
      "departments": [
        {"name": "Engineering", "employees": 2000},
        {"name": "Marketing", "employees": 500}
      ],
      "tags": ["tech", "innovation"],
      "revenue": null
    }
    """);

// Scalar access
String company = root.get("company").asText();           // "TechCorp"
int year = root.get("yearFounded").asInt();               // 2010
boolean isPublic = root.get("isPublic").asBoolean();      // false

// Nested object access
String city = root.get("headquarters").get("city").asText();  // "San Francisco"

// Array access
JsonNode departments = root.get("departments");
int deptCount = departments.size();                        // 2
String firstDept = departments.get(0).get("name").asText();  // "Engineering"

// Iterate over array elements
departments.elements().forEachRemaining(dept ->
    System.out.println(dept.get("name").asText() + ": " + dept.get("employees").asInt())
);

// JsonNode is Iterable — arrays yield elements, objects yield field values,
// scalars/null iterate zero times.
for (JsonNode dept : departments) {
    System.out.println(dept.get("name").asText());
}

// Stream over object fields
((ObjectNode) root).stream().forEach(entry ->
    System.out.println(entry.getKey() + " -> " + entry.getValue().type())
);

// Null checks
if (root.get("revenue").isNull()) {
    System.out.println("Revenue is null");
}

// Type checks
root.get("yearFounded").isNumber();    // true
root.get("company").isTextual();       // true
root.get("departments").isArray();     // true
root.get("company").isValueNode();     // true  (scalar, not a container)
root.get("tags").isEmpty();            // false (array has elements)

// In-place array replacement (returns the previous element)
ArrayNode tags = (ArrayNode) root.get("tags");
tags.set(0, new StringNode("redacted"));  // ["redacted", "innovation"]

// Safe access for missing keys
JsonNode missing = root.get("nonExistent");  // returns null
```

### 2. Fix and Pretty-Print Malformed JSON

```java
Json json = new Json();

String malformed = """
    {
      "name": John Doe
      "age": 30
      "skills": ["Java", "Python",]
      // This is a comment
      "active": true,
    }
    """;

// Fix into valid compact JSON
String fixed = json.fixJsonString(malformed);
// {"name":"John Doe","age":30,"skills":["Java","Python"],"active":true}

// Fix and pretty-print
String pretty = json.fixJsonString(malformed, true);
// {
//   "name": "John Doe",
//   "age": 30,
//   "skills": [
//     "Java",
//     "Python"
//   ],
//   "active": true
// }

// Pretty-print from an existing tree
JsonNode node = json.parse("{\"a\": 1, \"b\": [2, 3]}");
System.out.println(json.prettyPrint(node));
// {
//   "a": 1,
//   "b": [
//     2,
//     3
//   ]
// }
```

### 3. Flatten JSON to Dot-Notation Map

```java
Json json = new Json();

String input = """
    {
      "user": {
        "name": "Alice",
        "scores": [95, 87, 92]
      },
      "active": true
    }
    """;

Map<String, String> flat = json.flattenJSON(input);
// flat contains:
//   "user.name"      -> "Alice"
//   "user.scores[0]" -> "95"
//   "user.scores[1]" -> "87"
//   "user.scores[2]" -> "92"
//   "active"         -> "true"

// With a prefix
Map<String, String> prefixed = json.flattenJSON(input, "env.request");
// prefixed contains:
//   "env.request.user.name"      -> "Alice"
//   "env.request.user.scores[0]" -> "95"
//   ...

// Flatten from an existing tree
JsonNode node = json.parse(input);
Map<String, String> fromTree = json.flattenJSON(node);
```

### 4. Convert JSON to a POJO

```java
// Define your classes
class Address {
    String city;
    String zip;
}

class Person {
    String name;
    int age;
    Address address;
    List<String> tags;
    Map<String, Integer> scores;
}

Json json = new Json();
String input = """
    {
      "name": "Bob",
      "age": 25,
      "address": {"city": "NYC", "zip": "10001"},
      "tags": ["admin", "active"],
      "scores": {"math": 95, "eng": 88}
    }
    """;

// Parse and convert in one step
Person person = json.fromJson(input, Person.class);
person.name;                  // "Bob"
person.age;                   // 25
person.address.city;          // "NYC"
person.tags;                  // ["admin", "active"]
person.scores.get("math");   // 95

// Or convert from an existing tree
JsonNode node = json.parse(input);
Person p = json.toObject(node, Person.class);
```

**Supported target types for conversion:**
- Primitive types and their boxed counterparts (`int`/`Integer`, `long`/`Long`, etc.)
- `String`, `BigDecimal`, `BigInteger`
- Enums (matched by name)
- `List`, `Set`, `Collection` (with generic element types)
- `Map` (with generic key/value types)
- Arrays (e.g. `String[]`, `int[]`)
- POJOs (non-static, non-transient fields, matched by field name)
- Date/time types: `Date`, `Timestamp`, `Instant`, `LocalDate`, `LocalDateTime` (from epoch millis or ISO strings)

### 5. Handle Duplicate Keys

```java
Json json = new Json();

String input = """
    {
      "env": "dev",
      "env": "staging",
      "env": "prod"
    }
    """;

JsonNode root = json.parse(input);
JsonNode envNode = root.get("env");

// Duplicate values are merged into an ArrayNode
envNode.isArray();          // true
envNode.size();             // 3
envNode.get(0).asText();    // "dev"
envNode.get(1).asText();    // "staging"
envNode.get(2).asText();    // "prod"

// Re-serialized
json.stringify(root);
// {"env":["dev","staging","prod"]}
```

### 6. Handle Truncated JSON

```java
Json json = new Json();

// Unclosed object, array, and string
String truncated = """
    {
      "project": {
        "name": "Security Audit",
        "team": ["Alice", "Bob
    """;

JsonNode root = json.parse(truncated);

// Everything is auto-completed
root.get("project").get("name").asText();     // "Security Audit"
root.get("project").get("team").get(0).asText();  // "Alice"
root.get("project").get("team").get(1).asText();  // "Bob"

// Clean output
System.out.println(json.fixJsonString(truncated, true));
// {
//   "project": {
//     "name": "Security Audit",
//     "team": [
//       "Alice",
//       "Bob"
//     ]
//   }
// }
```
