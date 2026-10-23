package ai.traceable.laxjson4j.node;

import java.math.BigInteger;

public final class LongNode extends JsonNode {

  private final long value;

  public LongNode(long value) {
    this.value = value;
  }

  @Override
  public NodeType type() {
    return NodeType.LONG;
  }

  @Override
  public int asInt() {
    return (int) value;
  }

  @Override
  public long asLong() {
    return value;
  }

  @Override
  public double asDouble() {
    return (double) value;
  }

  @Override
  public Number asNumber() {
    return value;
  }

  @Override
  public BigInteger asBigInt() {
    return BigInteger.valueOf(value);
  }

  @Override
  public boolean asBoolean() {
    return value != 0L;
  }

  @Override
  public String asText() {
    return Long.toString(value);
  }

  @Override
  public void writeTo(StringBuilder sb) {
    sb.append(value);
  }

  @Override
  public String toString() {
    return Long.toString(value);
  }
}
