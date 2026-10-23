package ai.traceable.laxjson4j.node;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class BigIntegerNode extends JsonNode {

  private final BigInteger value;

  public BigIntegerNode(BigInteger value) {
    this.value = value;
  }

  @Override
  public NodeType type() {
    return NodeType.BIG_INTEGER;
  }

  @Override
  public int asInt() {
    return value.intValue();
  }

  @Override
  public long asLong() {
    return value.longValue();
  }

  @Override
  public double asDouble() {
    return value.doubleValue();
  }

  @Override
  public Number asNumber() {
    return value;
  }

  @Override
  public boolean asBoolean() {
    return !value.equals(BigInteger.ZERO);
  }

  @Override
  public BigInteger asBigInt() {
    return value;
  }

  @Override
  public BigDecimal asBigDecimal() {
    return new BigDecimal(value);
  }

  @Override
  public String asText() {
    return value.toString();
  }

  @Override
  public void writeTo(StringBuilder sb) {
    sb.append(value);
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
