package ai.traceable.laxjson4j;

import static org.junit.jupiter.api.Assertions.*;

import ai.traceable.laxjson4j.node.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JsonAccessorTest {

  private final Json json = new Json();

  private String readJsonFile(String fileName) throws IOException {
    return Files.readString(Paths.get("src/test/resources/" + fileName));
  }

  @Nested
  class ContainerAccessTests {

    @Test
    void objectFieldAccess() throws IOException {
      JsonNode root = json.parse(readJsonFile("test_json1.json"));
      assertInstanceOf(ObjectNode.class, root);

      assertEquals("Sample Data", root.get("name").asText());
      assertEquals(1.0D, root.get("version").asDouble());
      assertTrue(root.get("active").asBoolean());
      assertEquals("{}", root.get("emptyObject").toString());
      assertEquals("[]", root.get("emptyArray").toString());

      Set<String> expectedKeySet =
          Set.of(
              "name",
              "version",
              "active",
              "timestamp",
              "description",
              "users",
              "metrics",
              "tags",
              "values",
              "features",
              "emptyObject",
              "emptyArray",
              "nullField");
      assertEquals(expectedKeySet, ((ObjectNode) root).keySet());
    }

    @Test
    void arrayTraversal() throws IOException {
      JsonNode root = json.parse(readJsonFile("test_json1.json"));
      JsonNode usersNode = root.get("users");
      assertInstanceOf(ArrayNode.class, usersNode);

      JsonNode firstUser = usersNode.get(0);
      assertEquals(30, firstUser.get("age").asLong());
      assertEquals(1, firstUser.get("id").asInt());
      assertTrue(firstUser.get("verified").asBoolean());

      JsonNode valuesNode = root.get("values");
      assertEquals(100, valuesNode.get(0).asInt());
      assertEquals(200, valuesNode.get(1).asLong());
      assertEquals(-300.02e3, valuesNode.get(2).asDouble());
      assertEquals(3, valuesNode.size());
    }

    @Test
    void tagsArrayAccess() throws IOException {
      JsonNode root = json.parse(readJsonFile("test_json1.json"));
      JsonNode tagsNode = root.get("tags");

      assertTrue(tagsNode.get(3).isNull());
      assertTrue(tagsNode.get(4).asBoolean());
      assertEquals("null", tagsNode.get(3).toString());
      assertEquals("json", tagsNode.get(0).asText());

      List<JsonNode> listFromStream = ((ArrayNode) tagsNode).stream().collect(Collectors.toList());
      assertEquals("example", listFromStream.get(1).asText());
    }

    @Test
    void secondUserAccess() throws IOException {
      JsonNode root = json.parse(readJsonFile("test_json1.json"));
      JsonNode secondUser = root.get("users").get(1);

      assertEquals(45, secondUser.get("age").asLong());
      assertEquals(-500, secondUser.get("balance").asInt());
      assertFalse(secondUser.get("verified").asBoolean());
    }

    @Test
    void nonExistentKeyReturnsNull() throws IOException {
      JsonNode root = json.parse(readJsonFile("test_json1.json"));
      assertNull(root.get("nonExistentKey"));
    }

    @Test
    void outOfBoundsIndexReturnsNull() throws IOException {
      JsonNode root = json.parse(readJsonFile("test_json1.json"));
      JsonNode tags = root.get("tags");
      assertNull(tags.get(-1));
      assertNull(tags.get(100));
    }

    @Test
    void emptyContainerProperties() throws IOException {
      JsonNode root = json.parse(readJsonFile("test_json1.json"));
      assertEquals(0, root.get("emptyObject").size());
      assertEquals(0, root.get("emptyArray").size());
    }
  }
}
