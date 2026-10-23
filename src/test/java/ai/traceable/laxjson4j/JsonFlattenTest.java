package ai.traceable.laxjson4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.traceable.laxjson4j.node.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonFlattenTest {

  private final Json json = new Json();

  private String readJsonFile(String fileName) throws IOException {
    return Files.readString(Paths.get("src/test/resources/" + fileName));
  }

  @Test
  void testJSONOne() throws IOException {
    String jsonStr = readJsonFile("test_json1.json");
    Map<String, String> expected = new HashMap<>();

    expected.put("name", "Sample Data");
    expected.put("version", "1.0");
    expected.put("active", "true");
    expected.put("timestamp", "2025-03-03T12:34:56Z");
    expected.put("description", "This is a test JSON file with a variety of types.");

    expected.put("users[0].id", "1");
    expected.put("users[0].name", "Alice");
    expected.put("users[0].age", "30");
    expected.put("users[0].email", "alice@example.com");
    expected.put("users[0].verified", "true");
    expected.put("users[0].balance", "12345.67");
    expected.put("users[0].preferences.newsletter", "false");
    expected.put("users[0].preferences.theme", "dark");
    expected.put("users[0].preferences.notifications[0]", "email");
    expected.put("users[0].preferences.notifications[1]", "sms");

    expected.put("users[1].id", "2");
    expected.put("users[1].name", "Bob");
    expected.put("users[1].age", "45");
    expected.put("users[1].email", "bob@example.com");
    expected.put("users[1].verified", "false");
    expected.put("users[1].balance", "-500.5");
    expected.put("users[1].preferences.newsletter", "true");
    expected.put("users[1].preferences.theme", "light");

    expected.put("metrics.temperature", "22.5");
    expected.put("metrics.pressure", "1013");
    expected.put("metrics.humidity", "60");
    expected.put("metrics.coordinates.latitude", "37.7749");
    expected.put("metrics.coordinates.longitude", "-122.4194");
    expected.put("metrics.coordinates.elevation", "3050.0");

    expected.put("tags[0]", "json");
    expected.put("tags[1]", "example");
    expected.put("tags[2]", "test");
    expected.put("tags[3]", "null");
    expected.put("tags[4]", "true");

    expected.put("values[0]", "100");
    expected.put("values[1]", "200");
    expected.put("values[2]", "-300020.0");

    expected.put("features.enabled", "true");
    expected.put("features.count", "5");
    expected.put("features.subfeatures[0].name", "sub1");
    expected.put("features.subfeatures[0].enabled", "true");
    expected.put("features.subfeatures[1].name", "sub2");
    expected.put("features.subfeatures[1].enabled", "false");

    expected.put("nullField", "null");

    Map<String, String> actual = json.flattenJSON(jsonStr);
    assertEquals(expected, actual);
  }

  @Test
  void testJSONTwo() throws IOException {
    String jsonStr = readJsonFile("incorrect_json1.json");
    Map<String, String> expected = new HashMap<>();

    expected.put("timestamp", "2025-03-03T12:34:56");
    expected.put("nullField", "null");

    expected.put("users[0].id", "1");
    expected.put("users[0].name", "Alice");
    expected.put("users[0].age", "30.0");
    expected.put("users[0].verified", "true");
    expected.put("users[0].balance", "12345.67");
    expected.put("users[0].preferences.newsletter", "false");
    expected.put("users[0].preferences.theme", "dark");
    expected.put("users[0].preferences.notifications[0]", "email");
    expected.put("users[0].preferences.notifications[1]", "sms");

    expected.put("metrics.temperature", "22.5");
    expected.put("metrics.pressure", "1013");
    expected.put("metrics.humidity", "60");
    expected.put("metrics.coordinates.latitude", "37.7749");
    expected.put("metrics.coordinates.longitude", "-122.4194");
    expected.put("metrics.coordinates.elevation", "3050.0");

    Map<String, String> fromString = json.flattenJSON(jsonStr);
    assertEquals(expected, fromString);

    JsonNode parsed = json.parse(jsonStr);
    Map<String, String> fromTree = json.flattenJSON(parsed);
    assertEquals(expected, fromTree);
  }

  @Test
  void testEmptyObject() {
    JsonNode node = json.parse("{}");
    Map<String, String> result = json.flattenJSON(node);
    assertTrue(result.isEmpty());
  }

  @Test
  void testEmptyArray() {
    JsonNode node = json.parse("[]");
    Map<String, String> result = json.flattenJSON(node);
    assertTrue(result.isEmpty());
  }

  @Test
  void testNestedEmptyContainers() {
    JsonNode node = json.parse("{\"a\": {}, \"b\": []}");
    Map<String, String> result = json.flattenJSON(node);
    assertTrue(result.isEmpty());
  }

  @Test
  void testSimpleFlat() {
    JsonNode node = json.parse("{\"x\": 1, \"y\": \"hello\", \"z\": true}");
    Map<String, String> result = json.flattenJSON(node);
    assertEquals("1", result.get("x"));
    assertEquals("hello", result.get("y"));
    assertEquals("true", result.get("z"));
    assertEquals(3, result.size());
  }

  @Test
  void testPrefixWithNestedObject() {
    JsonNode node = json.parse("{\"a\": {\"b\": 1}, \"c\": 2}");
    Map<String, String> result = json.flattenJSON(node, "root.data");
    assertEquals("1", result.get("root.data.a.b"));
    assertEquals("2", result.get("root.data.c"));
    assertEquals(2, result.size());
  }

  @Test
  void testPrefixWithArray() {
    JsonNode node = json.parse("[10, 20]");
    Map<String, String> result = json.flattenJSON(node, "items");
    assertEquals("10", result.get("items[0]"));
    assertEquals("20", result.get("items[1]"));
    assertEquals(2, result.size());
  }

  @Test
  void testPrefixFromString() {
    Map<String, String> result = json.flattenJSON("{\"x\": 1}", "env.request");
    assertEquals("1", result.get("env.request.x"));
    assertEquals(1, result.size());
  }

  @Test
  void testEmptyPrefixMatchesNoPrefix() {
    JsonNode node = json.parse("{\"a\": 1}");
    assertEquals(json.flattenJSON(node), json.flattenJSON(node, ""));
  }
}
