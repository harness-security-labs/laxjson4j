package ai.traceable.laxjson4j.node;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class ObjectNode extends JsonNode {

  /**
   * Objects with at most this many fields are stored in parallel flat arrays. Linear scan over ≤8
   * cache-hot entries beats LinkedHashMap for typical JSON objects. Objects that grow beyond this
   * threshold are promoted to LinkedHashMap.
   */
  private static final int FLAT_CAPACITY = 8;

  // Flat storage — used while size <= FLAT_CAPACITY
  private String[] flatKeys = new String[FLAT_CAPACITY];
  private JsonNode[] flatVals = new JsonNode[FLAT_CAPACITY];
  private int flatSize = 0;

  // Overflow storage — allocated only when the object exceeds FLAT_CAPACITY fields
  private LinkedHashMap<String, JsonNode> overflow;

  // Tracks keys that appear more than once (rare, lenient-parse feature)
  private Set<String> duplicateKeys;

  @Override
  public NodeType type() {
    return NodeType.OBJECT;
  }

  public void put(String key, JsonNode value) {
    Objects.requireNonNull(key, "ObjectNode key must not be null");
    if (overflow != null) {
      putIntoMap(key, value);
      return;
    }

    // Search flat storage for an existing entry with this key
    for (int i = 0; i < flatSize; i++) {
      if (flatKeys[i].equals(key)) {
        mergeDuplicate(i, key, value);
        return;
      }
    }

    // New key: store flat if room, otherwise promote
    if (flatSize < FLAT_CAPACITY) {
      flatKeys[flatSize] = key;
      flatVals[flatSize] = value;
      flatSize++;
    } else {
      promote();
      putIntoMap(key, value);
    }
  }

  private void promote() {
    overflow = new LinkedHashMap<>(FLAT_CAPACITY * 2);
    for (int i = 0; i < flatSize; i++) {
      overflow.put(flatKeys[i], flatVals[i]);
    }
    flatKeys = null;
    flatVals = null;
  }

  private void putIntoMap(String key, JsonNode value) {
    JsonNode existing = overflow.get(key);
    if (existing == null) {
      overflow.put(key, value);
      return;
    }
    if (duplicateKeys == null) duplicateKeys = new HashSet<>();
    if (duplicateKeys.add(key)) {
      ArrayNode arr = new ArrayNode();
      arr.add(existing);
      arr.add(value);
      overflow.put(key, arr);
    } else {
      ((ArrayNode) existing).add(value);
    }
  }

  private void mergeDuplicate(int i, String key, JsonNode value) {
    JsonNode existing = flatVals[i];
    if (duplicateKeys == null) duplicateKeys = new HashSet<>();
    if (duplicateKeys.add(key)) {
      ArrayNode arr = new ArrayNode();
      arr.add(existing);
      arr.add(value);
      flatVals[i] = arr;
    } else {
      ((ArrayNode) existing).add(value);
    }
  }

  @Override
  public JsonNode get(String key) {
    if (overflow != null) return overflow.get(key);
    for (int i = 0; i < flatSize; i++) {
      if (flatKeys[i].equals(key)) return flatVals[i];
    }
    return null;
  }

  @Override
  public int size() {
    return overflow != null ? overflow.size() : flatSize;
  }

  public Set<String> keySet() {
    if (overflow != null) return overflow.keySet();
    LinkedHashSet<String> keys = new LinkedHashSet<>(flatSize * 2);
    for (int i = 0; i < flatSize; i++) keys.add(flatKeys[i]);
    return keys;
  }

  @Override
  public Iterator<Map.Entry<String, JsonNode>> fields() {
    if (overflow != null) return overflow.entrySet().iterator();
    return new FlatIterator(flatKeys, flatVals, flatSize);
  }

  /**
   * Iterates over the field <em>values</em> in insertion order. Use {@link #fields()} when you need
   * the keys.
   */
  @Override
  public Iterator<JsonNode> elements() {
    if (overflow != null) return overflow.values().iterator();
    return new ValIterator(flatVals, flatSize);
  }

  public Stream<Map.Entry<String, JsonNode>> stream() {
    if (overflow != null) return overflow.entrySet().stream();
    Iterable<Map.Entry<String, JsonNode>> iterable =
        () -> new FlatIterator(flatKeys, flatVals, flatSize);
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  @Override
  public void writeTo(StringBuilder sb) {
    sb.append('{');
    if (overflow != null) {
      boolean first = true;
      for (Map.Entry<String, JsonNode> entry : overflow.entrySet()) {
        if (!first) sb.append(',');
        first = false;
        sb.append('"');
        escapeStringTo(sb, entry.getKey());
        sb.append('"').append(':');
        entry.getValue().writeTo(sb);
      }
    } else {
      for (int i = 0; i < flatSize; i++) {
        if (i > 0) sb.append(',');
        sb.append('"');
        escapeStringTo(sb, flatKeys[i]);
        sb.append('"').append(':');
        flatVals[i].writeTo(sb);
      }
    }
    sb.append('}');
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(size() * 32);
    writeTo(sb);
    return sb.toString();
  }

  private static final class FlatIterator implements Iterator<Map.Entry<String, JsonNode>> {
    private final String[] keys;
    private final JsonNode[] vals;
    private final int size;
    private int pos;

    FlatIterator(String[] keys, JsonNode[] vals, int size) {
      this.keys = keys;
      this.vals = vals;
      this.size = size;
    }

    @Override
    public boolean hasNext() {
      return pos < size;
    }

    @Override
    public Map.Entry<String, JsonNode> next() {
      if (pos >= size) throw new NoSuchElementException();
      int i = pos++;
      return new AbstractMap.SimpleImmutableEntry<>(keys[i], vals[i]);
    }
  }

  private static final class ValIterator implements Iterator<JsonNode> {
    private final JsonNode[] vals;
    private final int size;
    private int pos;

    ValIterator(JsonNode[] vals, int size) {
      this.vals = vals;
      this.size = size;
    }

    @Override
    public boolean hasNext() {
      return pos < size;
    }

    @Override
    public JsonNode next() {
      if (pos >= size) throw new NoSuchElementException();
      return vals[pos++];
    }
  }
}
