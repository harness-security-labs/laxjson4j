package ai.traceable.laxjson4j.node;

public final class StringNode extends JsonNode {

  private final String value;

  public StringNode(String value) {
    this.value = value;
  }

  @Override
  public NodeType type() {
    return NodeType.STRING;
  }

  @Override
  public String asText() {
    return value;
  }

  @Override
  public boolean asBoolean() {
    return value.equalsIgnoreCase("true");
  }

  @Override
  public void writeTo(StringBuilder sb) {
    sb.append('"');
    escapeStringTo(sb, value);
    sb.append('"');
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(value.length() + 2);
    writeTo(sb);
    return sb.toString();
  }
}
