package ai.traceable.laxjson4j.node;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

public final class ArrayNode extends JsonNode {

  /**
   * Raw typed array avoids the ArrayList wrapper object, the untyped Object[] backing store, and
   * the implicit checkcast on every get(). Resizes double each time — same policy as ArrayList.
   */
  private JsonNode[] elements = new JsonNode[4];

  private int eSize = 0;

  @Override
  public NodeType type() {
    return NodeType.ARRAY;
  }

  public void add(JsonNode value) {
    if (eSize == elements.length) {
      elements = Arrays.copyOf(elements, elements.length << 1);
    }
    elements[eSize++] = value;
  }

  /**
   * Replaces the element at {@code index} with {@code value}, returning the previous element.
   * Matches Jackson's {@code ArrayNode.set(int, JsonNode)} semantics (returns the old value). No
   * resize occurs — the index must already exist.
   *
   * @param index zero-based index; must be in {@code [0, size())}
   * @param value the replacement node (must not be null)
   * @return the node previously at {@code index}
   * @throws IndexOutOfBoundsException if {@code index} is out of range
   * @throws NullPointerException if {@code value} is null (use {@link NullNode#INSTANCE} for JSON
   *     null)
   */
  public JsonNode set(int index, JsonNode value) {
    if (index < 0 || index >= eSize) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + eSize);
    }
    Objects.requireNonNull(value, "ArrayNode value must not be null; use NullNode.INSTANCE");
    JsonNode old = elements[index];
    elements[index] = value;
    return old;
  }

  @Override
  public JsonNode get(int index) {
    if (index < 0 || index >= eSize) {
      return null;
    }
    return elements[index];
  }

  @Override
  public int size() {
    return eSize;
  }

  @Override
  public Iterator<JsonNode> elements() {
    return new ElemIterator(elements, eSize);
  }

  public Stream<JsonNode> stream() {
    return Arrays.stream(elements, 0, eSize);
  }

  @Override
  public void writeTo(StringBuilder sb) {
    sb.append('[');
    for (int i = 0; i < eSize; i++) {
      if (i > 0) sb.append(',');
      elements[i].writeTo(sb);
    }
    sb.append(']');
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(eSize * 16);
    writeTo(sb);
    return sb.toString();
  }

  private static final class ElemIterator implements Iterator<JsonNode> {
    private final JsonNode[] arr;
    private final int size;
    private int pos;

    ElemIterator(JsonNode[] arr, int size) {
      this.arr = arr;
      this.size = size;
    }

    @Override
    public boolean hasNext() {
      return pos < size;
    }

    @Override
    public JsonNode next() {
      if (pos >= size) throw new NoSuchElementException();
      return arr[pos++];
    }
  }
}
