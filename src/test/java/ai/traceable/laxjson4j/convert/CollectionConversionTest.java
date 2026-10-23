package ai.traceable.laxjson4j.convert;

import static org.junit.jupiter.api.Assertions.*;

import ai.traceable.laxjson4j.exception.JsonConversionException;
import ai.traceable.laxjson4j.node.*;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CollectionConversionTest {

  static class Address {
    String city;
    String zip;
  }

  @SuppressWarnings("unused")
  List<String> stringListField;

  @SuppressWarnings("unused")
  List<Integer> integerListField;

  @SuppressWarnings("unused")
  Set<Integer> integerSetField;

  @SuppressWarnings("unused")
  List<Address> addressListField;

  @SuppressWarnings("unused")
  List<List<String>> nestedListField;

  private Type fieldType(String name) {
    try {
      return getClass().getDeclaredField(name).getGenericType();
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void listOfScalars() {
    ArrayNode arr = new ArrayNode();
    arr.add(new StringNode("a"));
    arr.add(new StringNode("b"));
    arr.add(new StringNode("c"));

    @SuppressWarnings("unchecked")
    List<String> strings = (List<String>) NodeConverter.convert(arr, fieldType("stringListField"));
    assertEquals(List.of("a", "b", "c"), strings);

    ArrayNode intArr = new ArrayNode();
    intArr.add(IntNode.valueOf(1));
    intArr.add(IntNode.valueOf(2));
    intArr.add(IntNode.valueOf(3));

    @SuppressWarnings("unchecked")
    List<Integer> ints =
        (List<Integer>) NodeConverter.convert(intArr, fieldType("integerListField"));
    assertEquals(List.of(1, 2, 3), ints);
  }

  @Test
  void setOfIntegers() {
    ArrayNode arr = new ArrayNode();
    arr.add(IntNode.valueOf(1));
    arr.add(IntNode.valueOf(2));
    arr.add(IntNode.valueOf(1));

    @SuppressWarnings("unchecked")
    Set<Integer> result = (Set<Integer>) NodeConverter.convert(arr, fieldType("integerSetField"));
    assertInstanceOf(Set.class, result);
    assertEquals(Set.of(1, 2), result);
  }

  @Test
  void listOfBeans() {
    ObjectNode a1 = new ObjectNode();
    a1.put("city", new StringNode("NYC"));
    a1.put("zip", new StringNode("10001"));
    ObjectNode a2 = new ObjectNode();
    a2.put("city", new StringNode("LA"));
    a2.put("zip", new StringNode("90001"));

    ArrayNode arr = new ArrayNode();
    arr.add(a1);
    arr.add(a2);

    @SuppressWarnings("unchecked")
    List<Address> addresses =
        (List<Address>) NodeConverter.convert(arr, fieldType("addressListField"));
    assertEquals(2, addresses.size());
    assertEquals("NYC", addresses.get(0).city);
    assertEquals("LA", addresses.get(1).city);
  }

  @Test
  void nestedCollections() {
    ArrayNode inner1 = new ArrayNode();
    inner1.add(new StringNode("a"));
    inner1.add(new StringNode("b"));
    ArrayNode inner2 = new ArrayNode();
    inner2.add(new StringNode("c"));

    ArrayNode outer = new ArrayNode();
    outer.add(inner1);
    outer.add(inner2);

    @SuppressWarnings("unchecked")
    List<List<String>> result =
        (List<List<String>>) NodeConverter.convert(outer, fieldType("nestedListField"));
    assertEquals(2, result.size());
    assertEquals(List.of("a", "b"), result.get(0));
    assertEquals(List.of("c"), result.get(1));
  }

  @Test
  void emptyAndNullHandling() {
    ArrayNode empty = new ArrayNode();

    @SuppressWarnings("unchecked")
    List<String> result = (List<String>) NodeConverter.convert(empty, fieldType("stringListField"));
    assertTrue(result.isEmpty());

    assertNull(NodeConverter.convert(NullNode.INSTANCE, fieldType("stringListField")));
    assertNull(NodeConverter.convert(null, fieldType("stringListField")));
  }

  @Test
  void nonArrayNodeThrows() {
    assertThrows(
        JsonConversionException.class,
        () -> NodeConverter.convert(new StringNode("x"), fieldType("stringListField")));

    assertThrows(
        JsonConversionException.class,
        () -> NodeConverter.convert(new ObjectNode(), fieldType("stringListField")));
  }
}
