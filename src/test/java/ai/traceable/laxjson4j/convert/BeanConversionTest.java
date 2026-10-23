package ai.traceable.laxjson4j.convert;

import static org.junit.jupiter.api.Assertions.*;

import ai.traceable.laxjson4j.exception.JsonConversionException;
import ai.traceable.laxjson4j.node.*;
import org.junit.jupiter.api.Test;

class BeanConversionTest {

  static class SimplePojo {
    String name;
    int age;
    boolean active;
    double score;
  }

  static class Address {
    String city;
    String zip;
  }

  static class PersonWithAddress {
    String name;
    Address address;
  }

  static class Parent {
    String parentField;
  }

  static class Child extends Parent {
    String childField;
  }

  static class NoDefaultCtor {
    String value;

    NoDefaultCtor(String value) {
      this.value = value;
    }
  }

  private ObjectNode objectNode() {
    return new ObjectNode();
  }

  @Test
  void simpleFlatPojo() {
    ObjectNode node = objectNode();
    node.put("name", new StringNode("Alice"));
    node.put("age", IntNode.valueOf(30));
    node.put("active", BooleanNode.TRUE);
    node.put("score", new DoubleNode(95.5));

    SimplePojo pojo = NodeConverter.convert(node, SimplePojo.class);

    assertEquals("Alice", pojo.name);
    assertEquals(30, pojo.age);
    assertTrue(pojo.active);
    assertEquals(95.5, pojo.score);
  }

  @Test
  void nestedPojo() {
    ObjectNode addrNode = objectNode();
    addrNode.put("city", new StringNode("NYC"));
    addrNode.put("zip", new StringNode("10001"));

    ObjectNode root = objectNode();
    root.put("name", new StringNode("Bob"));
    root.put("address", addrNode);

    PersonWithAddress person = NodeConverter.convert(root, PersonWithAddress.class);

    assertEquals("Bob", person.name);
    assertNotNull(person.address);
    assertEquals("NYC", person.address.city);
    assertEquals("10001", person.address.zip);
  }

  @Test
  void inheritedFields() {
    ObjectNode node = objectNode();
    node.put("childField", new StringNode("child"));
    node.put("parentField", new StringNode("parent"));

    Child child = NodeConverter.convert(node, Child.class);

    assertEquals("child", child.childField);
    assertEquals("parent", child.parentField);
  }

  @Test
  void missingAndExtraFieldsHandled() {
    ObjectNode node = objectNode();
    node.put("name", new StringNode("Alice"));
    node.put("unknownKey", new StringNode("ignored"));

    SimplePojo pojo = NodeConverter.convert(node, SimplePojo.class);

    assertEquals("Alice", pojo.name);
    assertEquals(0, pojo.age);
    assertFalse(pojo.active);
    assertEquals(0.0, pojo.score);
  }

  @Test
  void pojoWithoutDefaultConstructor() {
    ObjectNode node = objectNode();
    node.put("value", new StringNode("hello"));

    NoDefaultCtor pojo = NodeConverter.convert(node, NoDefaultCtor.class);

    assertEquals("hello", pojo.value);
  }

  @Test
  void nonObjectNodeThrowsForBeanTarget() {
    assertThrows(
        JsonConversionException.class,
        () -> NodeConverter.convert(new StringNode("text"), SimplePojo.class));

    ArrayNode arr = new ArrayNode();
    arr.add(IntNode.valueOf(1));
    assertThrows(JsonConversionException.class, () -> NodeConverter.convert(arr, SimplePojo.class));
  }
}
