package ai.traceable.laxjson4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.traceable.laxjson4j.node.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JsonSerializationTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  private final Json json = new Json();

  private String readJsonFile(String fileName) throws IOException {
    return Files.readString(Paths.get("src/test/resources/" + fileName));
  }

  @Nested
  class StringifyTests {

    @Test
    void testIncorrectJSONOne() throws IOException {
      String invalidJSON = readJsonFile("incorrect_json1.json");
      String expectedCorrectJSON = readJsonFile("corrected_json1.json");

      String correctedJson = json.stringify(json.parse(invalidJSON));
      assertEquals(expectedCorrectJSON, correctedJson);
    }

    @Test
    void testIncorrectJSONTwo() throws IOException {
      String invalidJSON = readJsonFile("incorrect_json2.json");
      String expectedCorrectJSON = readJsonFile("corrected_json2.json");

      String correctedJson = json.stringify(json.parse(invalidJSON));
      assertEquals(expectedCorrectJSON, correctedJson);
    }
  }

  @Nested
  class FixJsonStringTests {

    @Test
    void testJSONThreeCompact() throws IOException {
      String jsonStr = readJsonFile("test_json3.json");

      com.fasterxml.jackson.databind.JsonNode jacksonNode = mapper.readTree(jsonStr);
      String expectedJsonOutput = mapper.writeValueAsString(jacksonNode);
      String actualJsonOutput = json.fixJsonString(jsonStr);
      assertEquals(expectedJsonOutput, actualJsonOutput);
    }

    @Test
    void testJSONThreePrettyViaFixJsonString() throws IOException {
      String jsonStr = readJsonFile("test_json3.json");
      String expectedPrettyPrint = readJsonFile("test_json3_pretty.json");

      String actualPrettyPrint = json.fixJsonString(jsonStr, true);
      assertEquals(expectedPrettyPrint, actualPrettyPrint);
    }

    @Test
    void testJSONThreePrettyViaStringify() throws IOException {
      String jsonStr = readJsonFile("test_json3.json");
      String expectedPrettyPrint = readJsonFile("test_json3_pretty.json");

      JsonNode node = json.parse(jsonStr);
      String actualPrettyPrint = json.stringify(node, true);
      assertEquals(expectedPrettyPrint, actualPrettyPrint);
    }

    @Test
    void testJSONFour() throws IOException {
      String jsonStr = readJsonFile("test_json4.json");
      JsonNode node = json.parse(jsonStr);
      assertEquals(333, node.get("arraykey").get(0).asLong());

      com.fasterxml.jackson.databind.JsonNode jacksonNode = mapper.readTree(jsonStr);
      String expectedJsonOutput = mapper.writeValueAsString(jacksonNode);
      String actualJsonOutput = json.fixJsonString(jsonStr);
      assertEquals(expectedJsonOutput, actualJsonOutput);
    }

    @Test
    void testJSONWithDuplicateKeys() {
      String inputJson =
          "{\n"
              + "  \"arraykey\": [\n"
              + "    333,\n"
              + "    444,\n"
              + "    \"abc\"\n"
              + "  ],\n"
              + "  \"boolean\": true,\n"
              + "  \"color\": \"gold\",\n"
              + "  \"null\": null,\n"
              + "  \"number\": 123,\n"
              + "  \"object\": [\n"
              + "    {\"akey\": \"aval\"},\n"
              + "    {\"bkey\": \"bval1\"\n"
              + "      \"bkey\": bval2\n"
              + "    }\n"
              + "  ]\n"
              + "  // Duplicate Keys\n"
              + "  \"duplicateKey\": 1.0,\n"
              + "  \"duplicateKey\": [2.0, -3.0, 0],\n"
              + "  \"duplicateKey\": null\n"
              + "}\n";
      String expectedFixedJson =
          "{\"arraykey\":[333,444,\"abc\"],\"boolean\":true,\"color\":\"gold\",\"null\":null,\"number\":123,\"object\":[{\"akey\":\"aval\"},{\"bkey\":[\"bval1\",\"bval2\"]}],\"duplicateKey\":[1.0,[2.0,-3.0,0],null]}";
      assertEquals(expectedFixedJson, json.fixJsonString(inputJson));
    }
  }

  @Nested
  class PrettyPrintTests {

    @Test
    void prettyPrintSimpleObject() {
      JsonNode node = json.parse("{\"a\": 1, \"b\": \"hello\"}");
      String expected = "{\n  \"a\": 1,\n  \"b\": \"hello\"\n}";
      assertEquals(expected, json.prettyPrint(node));
    }

    @Test
    void prettyPrintSimpleArray() {
      JsonNode node = json.parse("[1, 2, 3]");
      String expected = "[\n  1,\n  2,\n  3\n]";
      assertEquals(expected, json.prettyPrint(node));
    }

    @Test
    void prettyPrintEmptyObject() {
      JsonNode node = json.parse("{}");
      String expected = "{\n\n}";
      assertEquals(expected, json.prettyPrint(node));
    }

    @Test
    void prettyPrintEmptyArray() {
      JsonNode node = json.parse("[]");
      String expected = "[\n\n]";
      assertEquals(expected, json.prettyPrint(node));
    }

    @Test
    void prettyPrintNestedStructure() {
      JsonNode node = json.parse("{\"outer\": {\"inner\": [1, 2]}}");
      String expected =
          "{\n"
              + "  \"outer\": {\n"
              + "    \"inner\": [\n"
              + "      1,\n"
              + "      2\n"
              + "    ]\n"
              + "  }\n"
              + "}";
      assertEquals(expected, json.prettyPrint(node));
    }

    @Test
    void prettyPrintNullValue() {
      JsonNode node = json.parse("{\"key\": null}");
      String expected = "{\n  \"key\": null\n}";
      assertEquals(expected, json.prettyPrint(node));
    }

    @Test
    void prettyPrintBooleanValues() {
      JsonNode node = json.parse("{\"t\": true, \"f\": false}");
      String expected = "{\n  \"t\": true,\n  \"f\": false\n}";
      assertEquals(expected, json.prettyPrint(node));
    }

    @Test
    void stringifyWithPrettyPrintFlag() {
      JsonNode node = json.parse("{\"x\": 1}");
      assertEquals(node.toString(), json.stringify(node, false));
      assertEquals(json.prettyPrint(node), json.stringify(node, true));
    }

    @Test
    void stringifyBooleanFalseMatchesStringifyNoArg() {
      // stringify(node, false) must delegate to stringify(node) — same object, same result
      JsonNode node = json.parse("{\"a\":1,\"b\":\"hello\",\"c\":true,\"d\":null}");
      assertEquals(json.stringify(node), json.stringify(node, false));
    }

    @Test
    void stringifyBooleanFalseOnScalarRoot() {
      // Scalar nodes rooted outside an object still round-trip correctly through
      // the writeTo path used by both stringify(node) and stringify(node, false)
      JsonNode node = json.parse("{\"v\":42}").get("v");
      String compact = json.stringify(node, false);
      assertEquals("42", compact);
      assertEquals(json.stringify(node), compact);
    }
  }
}
