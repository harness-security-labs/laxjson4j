package ai.traceable.laxjson4j;

import ai.traceable.laxjson4j.convert.NodeConverter;
import ai.traceable.laxjson4j.node.ArrayNode;
import ai.traceable.laxjson4j.node.JsonNode;
import ai.traceable.laxjson4j.node.ObjectNode;
import ai.traceable.laxjson4j.parser.NameTable;
import ai.traceable.laxjson4j.pool.ParserPool;
import java.util.HashMap;
import java.util.Map;

/**
 * Main entry point for the laxjson4j library. Provides methods to parse, serialize, flatten, and
 * convert JSON.
 *
 * <p>Each {@code Json} instance maintains its own shared {@link NameTable} for interning field
 * names and a {@link ThreadLocal} parser pool, making it safe to share a single instance across
 * threads. Typical usage is to create one instance at application startup and reuse it for all
 * parsing:
 *
 * <pre>{@code
 * Json json = new Json();
 * JsonNode node = json.parse(rawString);
 * }</pre>
 *
 * <p>The parser is fault-tolerant: malformed values (unquoted strings, trailing commas, comments)
 * are accepted wherever possible rather than rejected.
 */
public final class Json {

  private final NameTable nameTable;
  private final ThreadLocal<ParserPool> cache = new ThreadLocal<>();

  /**
   * Creates a new {@code Json} instance with its own name table and parser pool.
   *
   * <p>Instances are thread-safe and should be reused across parse calls for optimal performance,
   * as the internal name table accumulates interned field names that accelerate subsequent parses.
   */
  public Json() {
    this.nameTable = new NameTable();
  }

  /**
   * Parses a JSON string into a {@link JsonNode} tree.
   *
   * <p>The root of the returned tree is always an {@link ObjectNode} or {@link ArrayNode}. Scalar
   * roots (strings, numbers, booleans, null) are rejected with a {@link
   * ai.traceable.laxjson4j.exception.JsonParseException}.
   *
   * <p>The parser is lenient: it accepts unquoted keys and values, single-line ({@code //}) and
   * block ({@code /* ... * /}) comments, trailing commas, and other common deviations from strict
   * JSON.
   *
   * @param json the JSON string to parse; must not be {@code null}
   * @return the parsed tree rooted at an {@link ObjectNode} or {@link ArrayNode}
   * @throws IllegalArgumentException if {@code json} is {@code null}
   * @throws ai.traceable.laxjson4j.exception.JsonParseException if the input cannot be parsed into
   *     a valid object or array
   */
  public JsonNode parse(String json) {
    if (json == null) {
      throw new IllegalArgumentException("JSON string cannot be null");
    }
    ParserPool pool = acquire();
    try {
      return pool.parser().parse(json);
    } finally {
      release(pool);
    }
  }

  /**
   * Serializes a {@link JsonNode} tree into a compact JSON string with no extra whitespace.
   *
   * @param node the node to serialize
   * @return the compact JSON representation
   */
  public String stringify(JsonNode node) {
    StringBuilder sb = new StringBuilder();
    node.writeTo(sb);
    return sb.toString();
  }

  /**
   * Serializes a {@link JsonNode} tree into a JSON string, optionally pretty-printed with two-space
   * indentation.
   *
   * @param node the node to serialize
   * @param prettyPrint {@code true} to produce human-readable output with newlines and indentation;
   *     {@code false} for compact output
   * @return the JSON string representation
   */
  public String stringify(JsonNode node, boolean prettyPrint) {
    return prettyPrint ? prettyPrint(node) : stringify(node);
  }

  /**
   * Parses a potentially malformed JSON string and re-serializes it into valid, compact JSON.
   * Useful for sanitizing user-supplied or log-extracted JSON that may contain unquoted keys,
   * trailing commas, comments, or other non-standard constructs.
   *
   * <p>Equivalent to {@code parse(json).toString()}.
   *
   * @param json the JSON string to fix; must not be {@code null}
   * @return a well-formed compact JSON string
   * @throws IllegalArgumentException if {@code json} is {@code null}
   * @throws ai.traceable.laxjson4j.exception.JsonParseException if the input cannot be parsed into
   *     a valid object or array
   */
  public String fixJsonString(String json) {
    return parse(json).toString();
  }

  /**
   * Parses a potentially malformed JSON string and re-serializes it into valid JSON, optionally
   * pretty-printed.
   *
   * @param json the JSON string to fix; must not be {@code null}
   * @param prettyPrint {@code true} to produce human-readable output with newlines and two-space
   *     indentation; {@code false} for compact output
   * @return a well-formed JSON string
   * @throws IllegalArgumentException if {@code json} is {@code null}
   * @throws ai.traceable.laxjson4j.exception.JsonParseException if the input cannot be parsed into
   *     a valid object or array
   */
  public String fixJsonString(String json, boolean prettyPrint) {
    JsonNode node = parse(json);
    return prettyPrint ? prettyPrint(node) : node.toString();
  }

  /**
   * Serializes a {@link JsonNode} tree into a human-readable JSON string with two-space indentation
   * and newlines between elements.
   *
   * @param node the node to serialize
   * @return the pretty-printed JSON string
   */
  private static final int INDENT_CACHE_SIZE = 32;

  private static final String[] INDENT_CACHE = new String[INDENT_CACHE_SIZE];

  static {
    StringBuilder buf = new StringBuilder(INDENT_CACHE_SIZE * 2);
    for (int i = 0; i < INDENT_CACHE_SIZE; i++) {
      INDENT_CACHE[i] = buf.toString();
      buf.append("  ");
    }
  }

  private static String indent(int depth) {
    return depth < INDENT_CACHE_SIZE ? INDENT_CACHE[depth] : "  ".repeat(depth);
  }

  public String prettyPrint(JsonNode node) {
    StringBuilder sb = new StringBuilder();
    prettyPrint(node, sb, 0);
    return sb.toString();
  }

  private static void prettyPrint(JsonNode node, StringBuilder sb, int depth) {
    if (node instanceof ObjectNode) {
      ObjectNode obj = (ObjectNode) node;
      sb.append("{\n");
      String childIndent = indent(depth + 1);
      boolean first = true;
      var it = obj.fields();
      while (it.hasNext()) {
        var entry = it.next();
        if (!first) sb.append(",\n");
        first = false;
        sb.append(childIndent).append('"');
        JsonNode.escapeStringTo(sb, entry.getKey());
        sb.append("\": ");
        prettyPrint(entry.getValue(), sb, depth + 1);
      }
      sb.append('\n').append(indent(depth)).append('}');
    } else if (node instanceof ArrayNode) {
      ArrayNode arr = (ArrayNode) node;
      sb.append("[\n");
      String childIndent = indent(depth + 1);
      boolean first = true;
      var it = arr.elements();
      while (it.hasNext()) {
        if (!first) sb.append(",\n");
        first = false;
        sb.append(childIndent);
        prettyPrint(it.next(), sb, depth + 1);
      }
      sb.append('\n').append(indent(depth)).append(']');
    } else {
      node.writeTo(sb);
    }
  }

  private static final String SEP = ".";

  /**
   * Parses a JSON string and flattens the resulting tree into a single-level map of dot-separated
   * paths to leaf values.
   *
   * <p>Object keys are joined with {@code "."}; array indices are attached directly to the
   * preceding path segment as {@code "[i]"}. For example, the JSON:
   *
   * <pre>{@code {"a": {"b": [1, 2]}}} </pre>
   *
   * produces:
   *
   * <pre>{@code {"a.b[0]": "1", "a.b[1]": "2"}}</pre>
   *
   * @param json the JSON string to parse and flatten; must not be {@code null}
   * @return a map where keys are dot-separated paths and values are the string representations of
   *     the leaf nodes
   * @throws IllegalArgumentException if {@code json} is {@code null}
   * @throws ai.traceable.laxjson4j.exception.JsonParseException if the input cannot be parsed into
   *     a valid object or array
   */
  public Map<String, String> flattenJSON(String json) {
    return flattenJSON(parse(json), "");
  }

  /**
   * Parses a JSON string and flattens the resulting tree into a single-level map of dot-separated
   * paths to leaf values, prepending a user-supplied prefix to every key.
   *
   * <p>Object keys are joined with {@code "."}; array indices are attached directly to the
   * preceding path segment as {@code "[i]"}. For example, with prefix {@code "root.data"} and JSON:
   *
   * <pre>{@code {"a": {"b": [1, 2]}}} </pre>
   *
   * produces:
   *
   * <pre>{@code {"root.data.a.b[0]": "1", "root.data.a.b[1]": "2"}}</pre>
   *
   * @param json the JSON string to parse and flatten; must not be {@code null}
   * @param prefix a prefix to prepend to all keys in the output map; if empty, keys start at the
   *     root (e.g. {@code "a.b"}); if non-empty, it is joined with a {@code "."} separator (e.g.
   *     {@code "prefix.a.b"})
   * @return a map where keys are dot-separated paths (with the prefix) and values are the string
   *     representations of the leaf nodes
   * @throws IllegalArgumentException if {@code json} is {@code null}
   * @throws ai.traceable.laxjson4j.exception.JsonParseException if the input cannot be parsed into
   *     a valid object or array
   */
  public Map<String, String> flattenJSON(String json, String prefix) {
    return flattenJSON(parse(json), prefix);
  }

  /**
   * Flattens a {@link JsonNode} tree into a single-level map of dot-separated paths to leaf values.
   *
   * <p>Object keys are joined with {@code "."}; array indices are attached directly to the
   * preceding path segment as {@code "[i]"}. Leaf values are converted to strings via {@link
   * JsonNode#asText()}.
   *
   * @param node the root node to flatten
   * @return a map where keys are dot-separated paths and values are the string representations of
   *     the leaf nodes
   */
  public Map<String, String> flattenJSON(JsonNode node) {
    Map<String, String> result = new HashMap<>();
    flattenHelper(node, new StringBuilder(), result);
    return result;
  }

  /**
   * Flattens a {@link JsonNode} tree into a single-level map of dot-separated paths to leaf values,
   * prepending a user-supplied prefix to every key.
   *
   * <p>Object keys are joined with {@code "."}; array indices are attached directly to the
   * preceding path segment as {@code "[i]"}. Leaf values are converted to strings via {@link
   * JsonNode#asText()}.
   *
   * <p>For example, with prefix {@code "env.request"} and a node representing {@code {"user":
   * "alice"}}, the output map contains the entry {@code "env.request.user" → "alice"}.
   *
   * @param node the root node to flatten
   * @param prefix a prefix to prepend to all keys in the output map; if empty, keys start at the
   *     root; if non-empty, it is joined with a {@code "."} separator
   * @return a map where keys are dot-separated paths (with the prefix) and values are the string
   *     representations of the leaf nodes
   */
  public Map<String, String> flattenJSON(JsonNode node, String prefix) {
    Map<String, String> result = new HashMap<>();
    StringBuilder sb = new StringBuilder(prefix);
    flattenHelper(node, sb, result);
    return result;
  }

  private static void flattenHelper(
      JsonNode node, StringBuilder prefix, Map<String, String> result) {
    if (node instanceof ObjectNode) {
      ObjectNode obj = (ObjectNode) node;
      int baseLen = prefix.length();
      var it = obj.fields();
      while (it.hasNext()) {
        var entry = it.next();
        if (baseLen > 0) prefix.append(SEP);
        prefix.append(entry.getKey());
        flattenHelper(entry.getValue(), prefix, result);
        prefix.setLength(baseLen);
      }
    } else if (node instanceof ArrayNode) {
      ArrayNode arr = (ArrayNode) node;
      int baseLen = prefix.length();
      int i = 0;
      var it = arr.elements();
      while (it.hasNext()) {
        prefix.setLength(baseLen);
        prefix.append('[').append(i++).append(']');
        flattenHelper(it.next(), prefix, result);
      }
      prefix.setLength(baseLen);
    } else {
      result.put(prefix.toString(), node.asText());
    }
  }

  /**
   * Converts a {@link JsonNode} tree into a Java object of the specified type.
   *
   * <p>Supports primitive wrappers, {@link String}, {@link java.math.BigDecimal}, {@link
   * java.math.BigInteger}, date/time types, {@link java.util.List}, {@link java.util.Map}, arrays,
   * and POJOs with setter methods or public fields.
   *
   * @param <T> the target type
   * @param node the JSON node tree to convert
   * @param clazz the target class to convert into
   * @return an instance of {@code clazz} populated from the JSON node tree
   * @throws ai.traceable.laxjson4j.exception.JsonConversionException if the node structure is
   *     incompatible with the target type
   */
  public <T> T toObject(JsonNode node, Class<T> clazz) {
    return NodeConverter.convert(node, clazz);
  }

  /**
   * Parses a JSON string and converts the result directly into a Java object of the specified type.
   * This is a convenience method equivalent to {@code toObject(parse(json), clazz)}.
   *
   * @param <T> the target type
   * @param json the JSON string to parse; must not be {@code null}
   * @param clazz the target class to convert into
   * @return an instance of {@code clazz} populated from the parsed JSON
   * @throws IllegalArgumentException if {@code json} is {@code null}
   * @throws ai.traceable.laxjson4j.exception.JsonParseException if the input cannot be parsed into
   *     a valid object or array
   * @throws ai.traceable.laxjson4j.exception.JsonConversionException if the parsed node structure
   *     is incompatible with the target type
   */
  public <T> T fromJson(String json, Class<T> clazz) {
    return NodeConverter.convert(parse(json), clazz);
  }

  ParserPool acquire() {
    ParserPool pool = cache.get();
    if (pool != null) {
      cache.set(null);
      return pool;
    }
    return new ParserPool(nameTable);
  }

  void release(ParserPool pool) {
    pool.trimIfOversized();
    cache.set(pool);
  }
}
