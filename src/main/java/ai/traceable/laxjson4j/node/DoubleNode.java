package ai.traceable.laxjson4j.node;

public final class DoubleNode extends JsonNode {

  private final double value;

  public DoubleNode(double value) {
    this.value = value;
  }

  @Override
  public NodeType type() {
    return NodeType.DOUBLE;
  }

  @Override
  public int asInt() {
    return (int) value;
  }

  @Override
  public long asLong() {
    return (long) value;
  }

  @Override
  public double asDouble() {
    return value;
  }

  @Override
  public Number asNumber() {
    return value;
  }

  @Override
  public boolean asBoolean() {
    return value != 0.0;
  }

  @Override
  public String asText() {
    return Double.toString(value);
  }

  @Override
  public void writeTo(StringBuilder sb) {
    sb.append(value);
  }

  @Override
  public String toString() {
    return Double.toString(value);
  }
}
