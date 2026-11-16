// Portions adapted and modified from Jackson (jackson-databind, node/IntNode.java).
// Copyright 2007-, Tatu Saloranta. Licensed under the Apache License, Version 2.0. See NOTICE.md.
package ai.traceable.laxjson4j.node;

import java.math.BigInteger;

public final class IntNode extends JsonNode {

  private static final int CACHE_LOW = -1;
  private static final int CACHE_HIGH = 10;
  private static final IntNode[] CACHE = new IntNode[CACHE_HIGH - CACHE_LOW + 1];

  static {
    for (int i = CACHE_LOW; i <= CACHE_HIGH; i++) {
      CACHE[i - CACHE_LOW] = new IntNode(i);
    }
  }

  private final int value;

  IntNode(int value) {
    this.value = value;
  }

  public static IntNode valueOf(int v) {
    if (v >= CACHE_LOW && v <= CACHE_HIGH) {
      return CACHE[v - CACHE_LOW];
    }
    return new IntNode(v);
  }

  @Override
  public NodeType type() {
    return NodeType.INT;
  }

  @Override
  public int asInt() {
    return value;
  }

  @Override
  public long asLong() {
    return value;
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
  public BigInteger asBigInt() {
    return BigInteger.valueOf(value);
  }

  @Override
  public boolean asBoolean() {
    return value != 0;
  }

  @Override
  public String asText() {
    return Integer.toString(value);
  }

  @Override
  public void writeTo(StringBuilder sb) {
    sb.append(value);
  }

  @Override
  public String toString() {
    return Integer.toString(value);
  }
}
