package ai.traceable.laxjson4j.node;

public final class BooleanNode extends JsonNode {

  public static final BooleanNode TRUE = new BooleanNode(true);
  public static final BooleanNode FALSE = new BooleanNode(false);

  private final boolean value;

  private BooleanNode(boolean value) {
    this.value = value;
  }

  public static BooleanNode valueOf(boolean v) {
    return v ? TRUE : FALSE;
  }

  @Override
  public NodeType type() {
    return NodeType.BOOLEAN;
  }

  @Override
  public boolean asBoolean() {
    return value;
  }

  @Override
  public int asInt() {
    return value ? 1 : 0;
  }

  @Override
  public long asLong() {
    return value ? 1L : 0L;
  }

  @Override
  public double asDouble() {
    return value ? 1.0 : 0.0;
  }

  @Override
  public String asText() {
    return value ? "true" : "false";
  }

  @Override
  public void writeTo(StringBuilder sb) {
    sb.append(value ? "true" : "false");
  }

  @Override
  public String toString() {
    return value ? "true" : "false";
  }
}
