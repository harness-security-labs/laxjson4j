package ai.traceable.laxjson4j.node;

public final class NullNode extends JsonNode {

  public static final NullNode INSTANCE = new NullNode();

  private NullNode() {}

  @Override
  public NodeType type() {
    return NodeType.NULL;
  }

  @Override
  public boolean isNull() {
    return true;
  }

  @Override
  public String asText() {
    return "null";
  }

  @Override
  public void writeTo(StringBuilder sb) {
    sb.append("null");
  }

  @Override
  public String toString() {
    return "null";
  }
}
