package ai.traceable.laxjson4j.node;

import java.math.BigDecimal;

public final class BigDecimalNode extends JsonNode {

  private final String rawText;
  private volatile BigDecimal parsedValue;

  public BigDecimalNode(String rawText) {
    this.rawText = rawText;
  }

  @Override
  public NodeType type() {
    return NodeType.BIG_DECIMAL;
  }

  @Override
  public BigDecimal asBigDecimal() {
    BigDecimal v = parsedValue;
    if (v == null) {
      v = new BigDecimal(rawText);
      parsedValue = v;
    }
    return v;
  }

  @Override
  public int asInt() {
    return asBigDecimal().intValue();
  }

  @Override
  public long asLong() {
    return asBigDecimal().longValue();
  }

  @Override
  public double asDouble() {
    return Double.parseDouble(rawText);
  }

  @Override
  public boolean asBoolean() {
    return asBigDecimal().signum() != 0;
  }

  @Override
  public Number asNumber() {
    return asBigDecimal();
  }

  @Override
  public String asText() {
    return rawText;
  }

  @Override
  public void writeTo(StringBuilder sb) {
    sb.append(rawText);
  }

  @Override
  public String toString() {
    return rawText;
  }
}
