package ai.traceable.laxjson4j;

import static org.junit.jupiter.api.Assertions.*;

import ai.traceable.laxjson4j.node.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonConversionTest {

  static class Address {
    String city;
    String zip;
  }

  static class Person {
    String name;
    int age;
    Address address;
    List<String> tags;
    Map<String, Integer> scores;
  }

  private final Json json = new Json();

  @Test
  void toObjectWithSimplePojo() {
    JsonNode node = json.parse("{\"name\":\"Alice\",\"age\":30}");

    Person p = json.toObject(node, Person.class);

    assertEquals("Alice", p.name);
    assertEquals(30, p.age);
  }

  @Test
  void fromJsonWithNestedPojo() {
    String input =
        "{\"name\":\"Bob\",\"age\":25,"
            + "\"address\":{\"city\":\"NYC\",\"zip\":\"10001\"},"
            + "\"tags\":[\"admin\",\"active\"],"
            + "\"scores\":{\"math\":95,\"eng\":88}}";

    Person p = json.fromJson(input, Person.class);

    assertEquals("Bob", p.name);
    assertEquals(25, p.age);
    assertNotNull(p.address);
    assertEquals("NYC", p.address.city);
    assertEquals("10001", p.address.zip);
    assertEquals(List.of("admin", "active"), p.tags);
    assertEquals(95, p.scores.get("math"));
    assertEquals(88, p.scores.get("eng"));
  }

  @Test
  void nullAndEmptyHandling() {
    assertNull(json.toObject(null, Person.class));

    Person empty = json.fromJson("{}", Person.class);
    assertNotNull(empty);
    assertNull(empty.name);
    assertEquals(0, empty.age);
    assertNull(empty.address);
    assertNull(empty.tags);
  }
}
