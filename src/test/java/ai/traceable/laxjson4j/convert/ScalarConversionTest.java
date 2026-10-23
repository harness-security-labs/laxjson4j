package ai.traceable.laxjson4j.convert;

import static org.junit.jupiter.api.Assertions.*;

import ai.traceable.laxjson4j.node.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ScalarConversionTest {

  @Nested
  class NullHandling {
    @Test
    void nullAndNullNodeReturnDefaults() {
      // Null input
      assertEquals(0, (int) NodeConverter.convert(null, int.class));
      assertEquals(0L, (long) NodeConverter.convert(null, long.class));
      assertEquals(0.0, (double) NodeConverter.convert(null, double.class));
      assertEquals(0.0f, (float) NodeConverter.convert(null, float.class));
      assertFalse((boolean) NodeConverter.convert(null, boolean.class));
      assertEquals('\0', (char) NodeConverter.convert(null, char.class));
      assertEquals((byte) 0, (byte) NodeConverter.convert(null, byte.class));
      assertEquals((short) 0, (short) NodeConverter.convert(null, short.class));

      // NullNode for reference types
      assertNull(NodeConverter.convert(NullNode.INSTANCE, String.class));
      assertNull(NodeConverter.convert(NullNode.INSTANCE, Integer.class));
      assertNull(NodeConverter.convert(NullNode.INSTANCE, BigDecimal.class));

      // NullNode for primitives
      assertEquals(0, (int) NodeConverter.convert(NullNode.INSTANCE, int.class));
      assertFalse((boolean) NodeConverter.convert(NullNode.INSTANCE, boolean.class));
    }
  }

  @Nested
  class IntegerTypes {
    @Test
    void intAndBoxedIntFromNumericNodes() {
      assertEquals(42, NodeConverter.convert(IntNode.valueOf(42), int.class));
      assertEquals(42, NodeConverter.convert(IntNode.valueOf(42), Integer.class));
      assertEquals(100, NodeConverter.convert(new LongNode(100L), int.class));
      assertEquals(3, NodeConverter.convert(new DoubleNode(3.7), int.class));
    }

    @Test
    void longShortByteFromNumericNodes() {
      assertEquals(100L, NodeConverter.convert(IntNode.valueOf(100), long.class));
      assertEquals(100L, NodeConverter.convert(IntNode.valueOf(100), Long.class));
      assertEquals(9999999999L, NodeConverter.convert(new LongNode(9999999999L), long.class));

      assertEquals((short) 42, NodeConverter.convert(IntNode.valueOf(42), short.class));
      assertEquals((short) 42, NodeConverter.convert(IntNode.valueOf(42), Short.class));

      assertEquals((byte) 7, NodeConverter.convert(IntNode.valueOf(7), byte.class));
      assertEquals((byte) 7, NodeConverter.convert(IntNode.valueOf(7), Byte.class));
    }
  }

  @Nested
  class FloatingPointTypes {
    @Test
    void doubleAndFloatFromNumericNodes() {
      assertEquals(3.14, NodeConverter.convert(new DoubleNode(3.14), double.class));
      assertEquals(3.14, NodeConverter.convert(new DoubleNode(3.14), Double.class));
      assertEquals(5.0, NodeConverter.convert(IntNode.valueOf(5), double.class));

      assertEquals(5.0f, NodeConverter.convert(IntNode.valueOf(5), float.class));
      assertEquals(5.0f, NodeConverter.convert(IntNode.valueOf(5), Float.class));
      assertEquals(3.14f, NodeConverter.convert(new DoubleNode(3.14), float.class), 0.001f);
    }
  }

  @Nested
  class BooleanStringCharConversion {
    @Test
    void booleanFromVariousNodes() {
      assertTrue(NodeConverter.convert(BooleanNode.TRUE, boolean.class));
      assertFalse(NodeConverter.convert(BooleanNode.FALSE, boolean.class));
      assertTrue(NodeConverter.convert(BooleanNode.TRUE, Boolean.class));

      assertFalse(NodeConverter.convert(IntNode.valueOf(0), boolean.class));
      assertTrue(NodeConverter.convert(IntNode.valueOf(1), boolean.class));
    }

    @Test
    void stringFromVariousNodes() {
      assertEquals("hello", NodeConverter.convert(new StringNode("hello"), String.class));
      assertEquals("42", NodeConverter.convert(IntNode.valueOf(42), String.class));
      assertEquals("true", NodeConverter.convert(BooleanNode.TRUE, String.class));
      assertEquals("3.14", NodeConverter.convert(new DoubleNode(3.14), String.class));
    }

    @Test
    void charFromStringNode() {
      assertEquals('A', (char) NodeConverter.convert(new StringNode("A"), char.class));
      assertEquals('A', NodeConverter.convert(new StringNode("A"), Character.class));
      assertEquals('\0', (char) NodeConverter.convert(new StringNode(""), char.class));
      assertEquals('H', (char) NodeConverter.convert(new StringNode("Hello"), char.class));
    }
  }

  @Nested
  class BigNumberConversion {
    @Test
    void bigIntegerAndBigDecimal() {
      BigInteger big = new BigInteger("123456789012345678901234567890");
      assertEquals(big, NodeConverter.convert(new BigIntegerNode(big), BigInteger.class));
      assertEquals(
          BigInteger.valueOf(42), NodeConverter.convert(IntNode.valueOf(42), BigInteger.class));

      BigDecimal bd = new BigDecimal("3.14159265358979323846");
      assertEquals(
          bd, NodeConverter.convert(new BigDecimalNode(bd.toPlainString()), BigDecimal.class));
      // IntNode.asBigDecimal() returns null, fallback to new BigDecimal(asText())
      assertEquals(
          new BigDecimal("42"), NodeConverter.convert(IntNode.valueOf(42), BigDecimal.class));
    }
  }

  @Nested
  class SpecialCases {
    @Test
    void numberTargetType() {
      assertEquals(5, NodeConverter.convert(IntNode.valueOf(5), Number.class));
      assertEquals(3.14, NodeConverter.convert(new DoubleNode(3.14), Number.class));
    }

    @Test
    void jsonNodeIdentity() {
      IntNode intNode = IntNode.valueOf(5);
      assertSame(intNode, NodeConverter.convert(intNode, JsonNode.class));
      assertSame(intNode, NodeConverter.convert(intNode, IntNode.class));

      StringNode stringNode = new StringNode("test");
      assertSame(stringNode, NodeConverter.convert(stringNode, JsonNode.class));
    }
  }
}
