package ai.traceable.laxjson4j.convert;

import static org.junit.jupiter.api.Assertions.*;

import ai.traceable.laxjson4j.exception.JsonConversionException;
import ai.traceable.laxjson4j.node.*;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MapConversionTest {

  static class Address {
    String city;
    String zip;
  }

  @SuppressWarnings("unused")
  Map<String, String> stringStringMapField;

  @SuppressWarnings("unused")
  Map<String, Integer> stringIntMapField;

  @SuppressWarnings("unused")
  Map<String, Address> stringAddressMapField;

  @SuppressWarnings("unused")
  Map<String, List<String>> nestedMapField;

  private Type fieldType(String name) {
    try {
      return getClass().getDeclaredField(name).getGenericType();
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void simpleStringValueMaps() {
    ObjectNode node = new ObjectNode();
    node.put("k1", new StringNode("v1"));
    node.put("k2", new StringNode("v2"));

    @SuppressWarnings("unchecked")
    Map<String, String> strMap =
        (Map<String, String>) NodeConverter.convert(node, fieldType("stringStringMapField"));
    assertEquals(2, strMap.size());
    assertEquals("v1", strMap.get("k1"));
    assertEquals("v2", strMap.get("k2"));

    ObjectNode intNode = new ObjectNode();
    intNode.put("a", IntNode.valueOf(1));
    intNode.put("b", IntNode.valueOf(2));

    @SuppressWarnings("unchecked")
    Map<String, Integer> intMap =
        (Map<String, Integer>) NodeConverter.convert(intNode, fieldType("stringIntMapField"));
    assertEquals(1, intMap.get("a"));
    assertEquals(2, intMap.get("b"));
  }

  @Test
  void mapWithBeanValues() {
    ObjectNode addr = new ObjectNode();
    addr.put("city", new StringNode("NYC"));
    addr.put("zip", new StringNode("10001"));

    ObjectNode root = new ObjectNode();
    root.put("home", addr);

    @SuppressWarnings("unchecked")
    Map<String, Address> result =
        (Map<String, Address>) NodeConverter.convert(root, fieldType("stringAddressMapField"));
    assertEquals(1, result.size());
    Address a = result.get("home");
    assertNotNull(a);
    assertEquals("NYC", a.city);
    assertEquals("10001", a.zip);
  }

  @Test
  void nestedMapWithCollectionValues() {
    ArrayNode tags1 = new ArrayNode();
    tags1.add(new StringNode("a"));
    tags1.add(new StringNode("b"));
    ArrayNode tags2 = new ArrayNode();
    tags2.add(new StringNode("c"));

    ObjectNode root = new ObjectNode();
    root.put("group1", tags1);
    root.put("group2", tags2);

    @SuppressWarnings("unchecked")
    Map<String, List<String>> result =
        (Map<String, List<String>>) NodeConverter.convert(root, fieldType("nestedMapField"));
    assertEquals(List.of("a", "b"), result.get("group1"));
    assertEquals(List.of("c"), result.get("group2"));
  }

  @Test
  void emptyAndNullHandling() {
    ObjectNode empty = new ObjectNode();

    @SuppressWarnings("unchecked")
    Map<String, String> result =
        (Map<String, String>) NodeConverter.convert(empty, fieldType("stringStringMapField"));
    assertTrue(result.isEmpty());

    assertNull(NodeConverter.convert(NullNode.INSTANCE, fieldType("stringStringMapField")));
    assertNull(NodeConverter.convert(null, fieldType("stringStringMapField")));
  }

  @Test
  void nonObjectNodeThrows() {
    assertThrows(
        JsonConversionException.class,
        () -> NodeConverter.convert(new StringNode("x"), fieldType("stringStringMapField")));

    ArrayNode arr = new ArrayNode();
    arr.add(IntNode.valueOf(1));
    assertThrows(
        JsonConversionException.class,
        () -> NodeConverter.convert(arr, fieldType("stringStringMapField")));
  }
}
