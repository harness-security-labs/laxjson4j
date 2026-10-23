package ai.traceable.laxjson4j.convert;

import static org.junit.jupiter.api.Assertions.*;

import ai.traceable.laxjson4j.exception.JsonConversionException;
import ai.traceable.laxjson4j.node.*;
import org.junit.jupiter.api.Test;

class ArrayConversionTest {

  static class Address {
    String city;
    String zip;
  }

  @Test
  void objectArrays() {
    ArrayNode arr = new ArrayNode();
    arr.add(new StringNode("a"));
    arr.add(new StringNode("b"));
    arr.add(new StringNode("c"));

    String[] strings = NodeConverter.convert(arr, String[].class);
    assertArrayEquals(new String[] {"a", "b", "c"}, strings);

    ArrayNode intArr = new ArrayNode();
    intArr.add(IntNode.valueOf(10));
    intArr.add(IntNode.valueOf(20));

    Integer[] boxed = NodeConverter.convert(intArr, Integer[].class);
    assertArrayEquals(new Integer[] {10, 20}, boxed);
  }

  @Test
  void primitiveArrays() {
    ArrayNode intArr = new ArrayNode();
    intArr.add(IntNode.valueOf(1));
    intArr.add(IntNode.valueOf(2));
    intArr.add(IntNode.valueOf(3));

    int[] ints = NodeConverter.convert(intArr, int[].class);
    assertArrayEquals(new int[] {1, 2, 3}, ints);

    ArrayNode dblArr = new ArrayNode();
    dblArr.add(new DoubleNode(1.5));
    dblArr.add(new DoubleNode(2.5));

    double[] doubles = NodeConverter.convert(dblArr, double[].class);
    assertArrayEquals(new double[] {1.5, 2.5}, doubles);

    ArrayNode boolArr = new ArrayNode();
    boolArr.add(BooleanNode.TRUE);
    boolArr.add(BooleanNode.FALSE);

    boolean[] bools = NodeConverter.convert(boolArr, boolean[].class);
    assertArrayEquals(new boolean[] {true, false}, bools);
  }

  @Test
  void beanArrays() {
    ObjectNode a1 = new ObjectNode();
    a1.put("city", new StringNode("NYC"));
    a1.put("zip", new StringNode("10001"));
    ObjectNode a2 = new ObjectNode();
    a2.put("city", new StringNode("LA"));
    a2.put("zip", new StringNode("90001"));

    ArrayNode arr = new ArrayNode();
    arr.add(a1);
    arr.add(a2);

    Address[] addresses = NodeConverter.convert(arr, Address[].class);
    assertEquals(2, addresses.length);
    assertEquals("NYC", addresses[0].city);
    assertEquals("90001", addresses[1].zip);
  }

  @Test
  void emptyArrayConversion() {
    ArrayNode empty = new ArrayNode();

    String[] strings = NodeConverter.convert(empty, String[].class);
    assertEquals(0, strings.length);

    int[] ints = NodeConverter.convert(empty, int[].class);
    assertEquals(0, ints.length);
  }

  @Test
  void nonArrayNodeThrows() {
    assertThrows(
        JsonConversionException.class,
        () -> NodeConverter.convert(new StringNode("x"), String[].class));

    assertThrows(
        JsonConversionException.class, () -> NodeConverter.convert(new ObjectNode(), int[].class));
  }
}
