// ESCAPE_TABLE and escapeString adapted and modified from Gson (stream/JsonWriter.java).
// Copyright (C) 2010 Google Inc. Licensed under the Apache License, Version 2.0. See NOTICE.md.
package ai.traceable.laxjson4j.node;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public abstract class JsonNode implements Iterable<JsonNode> {

  // ── Escape table for JSON string serialization ──────────────────────
  static final String[] ESCAPE_TABLE = new String[128];

  static {
    for (int i = 0; i < 0x20; i++) {
      ESCAPE_TABLE[i] = String.format("\\u%04x", i);
    }
    ESCAPE_TABLE['"'] = "\\\"";
    ESCAPE_TABLE['\\'] = "\\\\";
    ESCAPE_TABLE['\b'] = "\\b";
    ESCAPE_TABLE['\t'] = "\\t";
    ESCAPE_TABLE['\n'] = "\\n";
    ESCAPE_TABLE['\f'] = "\\f";
    ESCAPE_TABLE['\r'] = "\\r";
  }

  public static String escapeString(String s) {
    int len = s.length();
    int start = 0;
    StringBuilder sb = null;

    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      String esc;
      if (c < 128 && (esc = ESCAPE_TABLE[c]) != null) {
        if (sb == null) {
          sb = new StringBuilder(len + 16);
        }
        if (i > start) {
          sb.append(s, start, i);
        }
        sb.append(esc);
        start = i + 1;
      }
    }

    if (sb == null) {
      return s;
    }
    if (start < len) {
      sb.append(s, start, len);
    }
    return sb.toString();
  }

  /**
   * Writes the JSON-escaped form of {@code s} directly into {@code sb} without allocating an
   * intermediate String. This is the zero-copy equivalent of {@code sb.append(escapeString(s))}.
   */
  public static void escapeStringTo(StringBuilder sb, String s) {
    int len = s.length();
    int start = 0;
    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      String esc;
      if (c < 128 && (esc = ESCAPE_TABLE[c]) != null) {
        if (i > start) sb.append(s, start, i);
        sb.append(esc);
        start = i + 1;
      }
    }
    if (start < len) sb.append(s, start, len);
  }

  // ── Type ────────────────────────────────────────────────────────────

  public abstract NodeType type();

  // ── Type checks ─────────────────────────────────────────────────────

  public boolean isObject() {
    return type() == NodeType.OBJECT;
  }

  public boolean isArray() {
    return type() == NodeType.ARRAY;
  }

  public boolean isNull() {
    return type() == NodeType.NULL;
  }

  public boolean isNumber() {
    switch (type()) {
      case INT:
      case LONG:
      case DOUBLE:
      case BIG_INTEGER:
      case BIG_DECIMAL:
        return true;
      default:
        return false;
    }
  }

  public boolean isTextual() {
    return type() == NodeType.STRING;
  }

  public boolean isBoolean() {
    return type() == NodeType.BOOLEAN;
  }

  /**
   * Returns {@code true} if this node is a scalar value (string, number, boolean, or null) — i.e.
   * not an object or array. {@code NullNode} is considered a value node.
   */
  public boolean isValueNode() {
    return !isObject() && !isArray();
  }

  /**
   * Returns {@code true} if this node has no children. For containers this means an empty object or
   * array; scalars and null report a {@link #size()} of 0 and are therefore also considered empty.
   */
  public boolean isEmpty() {
    return size() == 0;
  }

  // ── Value accessors (safe defaults) ─────────────────────────────────

  public String asText() {
    return null;
  }

  public int asInt() {
    return 0;
  }

  public long asLong() {
    return 0L;
  }

  public double asDouble() {
    return 0.0;
  }

  public BigInteger asBigInt() {
    return null;
  }

  public BigDecimal asBigDecimal() {
    return null;
  }

  public Number asNumber() {
    return null;
  }

  public boolean asBoolean() {
    return false;
  }

  // ── Container accessors (overridden by ObjectNode / ArrayNode) ──────

  public JsonNode get(String key) {
    return null;
  }

  public JsonNode get(int index) {
    return null;
  }

  public int size() {
    return 0;
  }

  public Iterator<JsonNode> elements() {
    return Collections.emptyIterator();
  }

  /**
   * Iterates over child values, delegating to {@link #elements()}. Arrays yield their elements and
   * objects yield their field values; scalars and null iterate zero times.
   */
  @Override
  public Iterator<JsonNode> iterator() {
    return elements();
  }

  public Iterator<Map.Entry<String, JsonNode>> fields() {
    return Collections.emptyIterator();
  }

  // ── Serialization ───────────────────────────────────────────────────

  /**
   * Appends the compact JSON representation of this node directly into {@code sb}. No intermediate
   * {@code String} objects are allocated. Callers that need a {@code String} should use {@link
   * #toString()}, which delegates here.
   */
  public abstract void writeTo(StringBuilder sb);

  @Override
  public abstract String toString();
}
