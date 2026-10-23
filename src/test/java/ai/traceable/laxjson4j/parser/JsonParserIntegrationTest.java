package ai.traceable.laxjson4j.parser;

import static org.junit.jupiter.api.Assertions.*;

import ai.traceable.laxjson4j.exception.JsonParseException;
import ai.traceable.laxjson4j.node.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JsonParserIntegrationTest {

  private static JsonParser parser(String input, int bufSize) {
    JsonParser p = new JsonParser(new char[bufSize], new CharBuf(64), new NameTable());
    p.init(input);
    return p;
  }

  private static JsonParser parser(String input) {
    return parser(input, Math.max(input.length() + 16, 64));
  }

  @Nested
  class ValidJsonTests {
    @Test
    void simpleObject() {
      JsonParser p = parser("{\"key\": \"value\"}");
      JsonNode root = p.parse();
      assertInstanceOf(ObjectNode.class, root);
      assertEquals("value", root.get("key").asText());
    }

    @Test
    void simpleArray() {
      JsonParser p = parser("[1, 2, 3]");
      JsonNode root = p.parse();
      assertInstanceOf(ArrayNode.class, root);
      assertEquals(3, root.size());
      assertEquals(1, root.get(0).asInt());
      assertEquals(2, root.get(1).asInt());
      assertEquals(3, root.get(2).asInt());
    }

    @Test
    void nestedObjectInObject() {
      JsonParser p = parser("{\"a\": {\"b\": 1}}");
      JsonNode root = p.parse();
      assertEquals(1, root.get("a").get("b").asInt());
    }

    @Test
    void nestedArrayInObject() {
      JsonParser p = parser("{\"a\": [1, 2]}");
      JsonNode root = p.parse();
      assertInstanceOf(ArrayNode.class, root.get("a"));
      assertEquals(2, root.get("a").size());
      assertEquals(1, root.get("a").get(0).asInt());
    }

    @Test
    void nestedObjectInArray() {
      JsonParser p = parser("[{\"x\": 10}, {\"y\": 20}]");
      JsonNode root = p.parse();
      assertEquals(2, root.size());
      assertEquals(10, root.get(0).get("x").asInt());
      assertEquals(20, root.get(1).get("y").asInt());
    }

    @Test
    void deeplyNested() {
      JsonParser p = parser("{\"a\": {\"b\": {\"c\": [1, [2, [3]]]}}}");
      JsonNode root = p.parse();
      JsonNode c = root.get("a").get("b").get("c");
      assertInstanceOf(ArrayNode.class, c);
      assertEquals(1, c.get(0).asInt());
      assertEquals(2, c.get(1).get(0).asInt());
      assertEquals(3, c.get(1).get(1).get(0).asInt());
    }

    @Test
    void allValueTypes() {
      String json =
          "{\"s\": \"hello\", \"i\": 42, \"l\": 9999999999, \"d\": 3.14, "
              + "\"bt\": true, \"bf\": false, \"n\": null}";
      JsonParser p = parser(json);
      JsonNode root = p.parse();
      assertEquals("hello", root.get("s").asText());
      assertEquals(42, root.get("i").asInt());
      assertEquals(9999999999L, root.get("l").asLong());
      assertEquals(3.14, root.get("d").asDouble(), 0.001);
      assertTrue(root.get("bt").asBoolean());
      assertFalse(root.get("bf").asBoolean());
      assertTrue(root.get("n").isNull());
    }

    @Test
    void emptyObject() {
      JsonParser p = parser("{}");
      JsonNode root = p.parse();
      assertInstanceOf(ObjectNode.class, root);
      assertEquals(0, root.size());
    }

    @Test
    void emptyArray() {
      JsonParser p = parser("[]");
      JsonNode root = p.parse();
      assertInstanceOf(ArrayNode.class, root);
      assertEquals(0, root.size());
    }

    @Test
    void nestedEmptyContainers() {
      JsonParser p = parser("{\"a\": {}, \"b\": []}");
      JsonNode root = p.parse();
      assertInstanceOf(ObjectNode.class, root.get("a"));
      assertEquals(0, root.get("a").size());
      assertInstanceOf(ArrayNode.class, root.get("b"));
      assertEquals(0, root.get("b").size());
    }

    @Test
    void arrayOfArrays() {
      JsonParser p = parser("[[1, 2], [3, 4], [5]]");
      JsonNode root = p.parse();
      assertEquals(3, root.size());
      assertEquals(2, root.get(0).size());
      assertEquals(1, root.get(0).get(0).asInt());
      assertEquals(5, root.get(2).get(0).asInt());
    }

    @Test
    void objectWithWhitespace() {
      JsonParser p = parser("  {  \"key\"  :  \"value\"  }  ");
      JsonNode root = p.parse();
      assertEquals("value", root.get("key").asText());
    }

    @Test
    void multilineObject() {
      String json = "{\n  \"name\": \"test\",\n  \"count\": 5\n}";
      JsonParser p = parser(json);
      JsonNode root = p.parse();
      assertEquals("test", root.get("name").asText());
      assertEquals(5, root.get("count").asInt());
    }

    @Test
    void stringWithEscapes() {
      JsonParser p = parser("{\"msg\": \"hello\\nworld\\t!\"}");
      JsonNode root = p.parse();
      assertEquals("hello\nworld\t!", root.get("msg").asText());
    }

    @Test
    void negativeNumbers() {
      JsonParser p = parser("[-1, -42, -3.14]");
      JsonNode root = p.parse();
      assertEquals(-1, root.get(0).asInt());
      assertEquals(-42, root.get(1).asInt());
      assertEquals(-3.14, root.get(2).asDouble(), 0.001);
    }

    @Test
    void exponentNumbers() {
      JsonParser p = parser("[1e5, 2.5E-3, -1.0e+2]");
      JsonNode root = p.parse();
      assertEquals(1e5, root.get(0).asDouble(), 1.0);
      assertEquals(2.5e-3, root.get(1).asDouble(), 0.0001);
      assertEquals(-1.0e+2, root.get(2).asDouble(), 0.1);
    }

    @Test
    void bigIntegerValue() {
      JsonParser p = parser("[99999999999999999999]");
      JsonNode root = p.parse();
      assertInstanceOf(BigIntegerNode.class, root.get(0));
      assertEquals("99999999999999999999", root.get(0).asText());
    }
  }

  @Nested
  class ErrorToleranceTests {
    @Test
    void missingCommasBetweenObjectMembers() {
      JsonParser p = parser("{\"a\": 1\n\"b\": 2}");
      JsonNode root = p.parse();
      assertEquals(1, root.get("a").asInt());
      assertEquals(2, root.get("b").asInt());
    }

    @Test
    void missingCommasBetweenArrayElements() {
      JsonParser p = parser("[1\n2\n3]");
      JsonNode root = p.parse();
      assertEquals(3, root.size());
      assertEquals(1, root.get(0).asInt());
      assertEquals(2, root.get(1).asInt());
      assertEquals(3, root.get(2).asInt());
    }

    @Test
    void missingColonAfterKeyThrows() {
      JsonParser p = parser("{\"key\" \"value\"}");
      assertThrows(JsonParseException.class, p::parse);
    }

    @Test
    void missingColonAndCommaThrows() {
      JsonParser p = parser("{\"a\" 1\n\"b\" 2}");
      assertThrows(JsonParseException.class, p::parse);
    }

    @Test
    void trailingCommaInObject() {
      JsonParser p = parser("{\"a\": 1,}");
      JsonNode root = p.parse();
      assertEquals(1, root.get("a").asInt());
      assertEquals(1, root.size());
    }

    @Test
    void trailingCommaInArray() {
      JsonParser p = parser("[1, 2,]");
      JsonNode root = p.parse();
      assertEquals(2, root.size());
      assertEquals(1, root.get(0).asInt());
      assertEquals(2, root.get(1).asInt());
    }

    @Test
    void duplicateKeys() {
      JsonParser p = parser("{\"a\": 1, \"a\": 2}");
      JsonNode root = p.parse();
      JsonNode a = root.get("a");
      assertInstanceOf(ArrayNode.class, a);
      assertEquals(2, a.size());
      assertEquals(1, a.get(0).asInt());
      assertEquals(2, a.get(1).asInt());
    }

    @Test
    void booleanAndNullValues() {
      JsonParser p = parser("{\"t\": true, \"f\": false, \"n\": null}");
      JsonNode root = p.parse();
      assertSame(BooleanNode.TRUE, root.get("t"));
      assertSame(BooleanNode.FALSE, root.get("f"));
      assertSame(NullNode.INSTANCE, root.get("n"));
    }

    @Test
    void multipleTrailingCommas() {
      JsonParser p = parser("[1,,,2]");
      JsonNode root = p.parse();
      assertEquals(2, root.size());
      assertEquals(1, root.get(0).asInt());
      assertEquals(2, root.get(1).asInt());
    }
  }

  @Nested
  class TruncatedJsonTests {
    @Test
    void objectWithoutClosingBrace() {
      JsonParser p = parser("{\"key\": \"val\"");
      JsonNode root = p.parse();
      assertInstanceOf(ObjectNode.class, root);
      assertEquals("val", root.get("key").asText());
    }

    @Test
    void arrayWithoutClosingBracket() {
      JsonParser p = parser("[1, 2, 3");
      JsonNode root = p.parse();
      assertInstanceOf(ArrayNode.class, root);
      assertEquals(3, root.size());
      assertEquals(1, root.get(0).asInt());
      assertEquals(3, root.get(2).asInt());
    }

    @Test
    void nestedOuterTruncated() {
      JsonParser p = parser("{\"a\": {\"b\": 1}");
      JsonNode root = p.parse();
      assertInstanceOf(ObjectNode.class, root);
      assertEquals(1, root.get("a").get("b").asInt());
    }

    @Test
    void keyWithNoValueTruncated() {
      JsonParser p = parser("{\"key\":");
      JsonNode root = p.parse();
      assertInstanceOf(ObjectNode.class, root);
      assertEquals(0, root.size());
    }

    @Test
    void nestedArrayTruncated() {
      JsonParser p = parser("[1, [2, 3");
      JsonNode root = p.parse();
      assertInstanceOf(ArrayNode.class, root);
      assertEquals(2, root.size());
      assertEquals(1, root.get(0).asInt());
      assertInstanceOf(ArrayNode.class, root.get(1));
      assertEquals(2, root.get(1).size());
    }

    @Test
    void deeplyNestedTruncated() {
      JsonParser p = parser("{\"a\": {\"b\": [1, {\"c\": 2");
      JsonNode root = p.parse();
      assertInstanceOf(ObjectNode.class, root);
      JsonNode b = root.get("a").get("b");
      assertInstanceOf(ArrayNode.class, b);
      assertEquals(1, b.get(0).asInt());
      assertEquals(2, b.get(1).get("c").asInt());
    }

    @Test
    void emptyObjectTruncated() {
      JsonParser p = parser("{");
      JsonNode root = p.parse();
      assertInstanceOf(ObjectNode.class, root);
      assertEquals(0, root.size());
    }

    @Test
    void emptyArrayTruncated() {
      JsonParser p = parser("[");
      JsonNode root = p.parse();
      assertInstanceOf(ArrayNode.class, root);
      assertEquals(0, root.size());
    }
  }

  @Nested
  class RootValidationTests {
    @Test
    void stringRootThrows() {
      JsonParser p = parser("\"hello\"");
      assertThrows(JsonParseException.class, p::parse);
    }

    @Test
    void numberRootThrows() {
      JsonParser p = parser("42");
      assertThrows(JsonParseException.class, p::parse);
    }

    @Test
    void trueRootThrows() {
      JsonParser p = parser("true");
      assertThrows(JsonParseException.class, p::parse);
    }

    @Test
    void falseRootThrows() {
      JsonParser p = parser("false");
      assertThrows(JsonParseException.class, p::parse);
    }

    @Test
    void nullRootThrows() {
      JsonParser p = parser("null");
      assertThrows(JsonParseException.class, p::parse);
    }

    @Test
    void emptyInputThrows() {
      JsonParser p = parser("");
      assertThrows(JsonParseException.class, p::parse);
    }

    @Test
    void whitespaceOnlyThrows() {
      JsonParser p = parser("   \n\t  ");
      assertThrows(JsonParseException.class, p::parse);
    }
  }

  @Nested
  class BufferBoundaryIntegrationTests {
    @Test
    void smallObjectWithTinyBuffer() {
      JsonParser p = parser("{\"key\": \"value\"}", 8);
      JsonNode root = p.parse();
      assertEquals("value", root.get("key").asText());
    }

    @Test
    void arrayWithTinyBuffer() {
      JsonParser p = parser("[1, 2, 3, 4, 5]", 8);
      JsonNode root = p.parse();
      assertEquals(5, root.size());
      assertEquals(5, root.get(4).asInt());
    }

    @Test
    void nestedWithTinyBuffer() {
      JsonParser p = parser("{\"a\": {\"b\": [1, 2]}}", 8);
      JsonNode root = p.parse();
      assertEquals(2, root.get("a").get("b").size());
    }

    @Test
    void keyValueStraddlesBoundary() {
      JsonParser p = parser("{\"abcdefg\": \"hijklmn\"}", 10);
      JsonNode root = p.parse();
      assertEquals("hijklmn", root.get("abcdefg").asText());
    }

    @Test
    void multipleObjectMembersSmallBuffer() {
      String json = "{\"name\": \"alice\", \"age\": 30, \"active\": true}";
      JsonParser p = parser(json, 12);
      JsonNode root = p.parse();
      assertEquals("alice", root.get("name").asText());
      assertEquals(30, root.get("age").asInt());
      assertTrue(root.get("active").asBoolean());
    }

    @Test
    void truncatedWithSmallBuffer() {
      JsonParser p = parser("{\"key\": \"value\"", 8);
      JsonNode root = p.parse();
      assertInstanceOf(ObjectNode.class, root);
      assertEquals("value", root.get("key").asText());
    }

    @Test
    void largeArraySmallBuffer() {
      StringBuilder sb = new StringBuilder("[");
      for (int i = 0; i < 50; i++) {
        if (i > 0) sb.append(",");
        sb.append(i);
      }
      sb.append("]");
      JsonParser p = parser(sb.toString(), 16);
      JsonNode root = p.parse();
      assertEquals(50, root.size());
      assertEquals(0, root.get(0).asInt());
      assertEquals(49, root.get(49).asInt());
    }
  }

  @Nested
  class CommentIntegrationTests {
    @Test
    void blockCommentInObjectValue() {
      JsonParser p = parser("{\"key\": /* comment */ \"value\"}");
      JsonNode root = p.parse();
      assertEquals("value", root.get("key").asText());
    }

    @Test
    void lineCommentInArray() {
      JsonParser p = parser("[1, // comment\n 2]");
      JsonNode root = p.parse();
      assertEquals(2, root.size());
      assertEquals(1, root.get(0).asInt());
      assertEquals(2, root.get(1).asInt());
    }

    @Test
    void commentBeforeClosingBrace() {
      JsonParser p = parser("{\"a\": 1, /* trailing */ }");
      JsonNode root = p.parse();
      assertEquals(1, root.get("a").asInt());
    }

    @Test
    void commentBeforeClosingBracket() {
      JsonParser p = parser("[1, 2 // end\n]");
      JsonNode root = p.parse();
      assertEquals(2, root.size());
    }

    @Test
    void commentBetweenKeyAndColon() {
      JsonParser p = parser("{\"key\" /* x */ : \"value\"}");
      JsonNode root = p.parse();
      assertEquals("value", root.get("key").asText());
    }

    @Test
    void multipleCommentsInObject() {
      String json = "{ // header\n\"a\": 1, // first\n\"b\": 2\n// second\n}";
      JsonParser p = parser(json);
      JsonNode root = p.parse();
      assertEquals(1, root.get("a").asInt());
      assertEquals(2, root.get("b").asInt());
    }
  }

  @Nested
  class ObjectKeyRejectionTests {
    @Test
    void multipleStrayCommasInObject() {
      JsonParser p = parser("{,,\"a\": 1,,,\"b\": 2,,}");
      JsonNode root = p.parse();
      assertEquals(2, root.size());
      assertEquals(1, root.get("a").asInt());
      assertEquals(2, root.get("b").asInt());
    }

    @Test
    void colonInKeyPositionThrows() {
      JsonParser p = parser("{: \"value\"}");
      assertThrows(JsonParseException.class, p::parse);
    }

    @Test
    void bracketInKeyPositionThrows() {
      JsonParser p = parser("{[\"a\"]}");
      assertThrows(JsonParseException.class, p::parse);
    }

    @Test
    void braceInKeyPositionThrows() {
      JsonParser p = parser("{{\"a\": 1}}");
      assertThrows(JsonParseException.class, p::parse);
    }

    @Test
    void closeBracketInKeyPositionThrows() {
      JsonParser p = parser("{] \"a\": 1}");
      assertThrows(JsonParseException.class, p::parse);
    }

    @Test
    void errorMessageContainsChar() {
      JsonParser p = parser("{: \"a\": 1}");
      JsonParseException ex = assertThrows(JsonParseException.class, p::parse);
      assertTrue(ex.getMessage().contains(":"));
    }
  }

  @Nested
  class ArrayStraySeparatorTests {
    @Test
    void consecutiveCommas() {
      JsonParser p = parser("[1,,2]");
      JsonNode root = p.parse();
      assertEquals(2, root.size());
      assertEquals(1, root.get(0).asInt());
      assertEquals(2, root.get(1).asInt());
    }

    @Test
    void leadingComma() {
      JsonParser p = parser("[,1,2]");
      JsonNode root = p.parse();
      assertEquals(2, root.size());
      assertEquals(1, root.get(0).asInt());
      assertEquals(2, root.get(1).asInt());
    }

    @Test
    void multipleStrayCommas() {
      JsonParser p = parser("[1,,,2]");
      JsonNode root = p.parse();
      assertEquals(2, root.size());
      assertEquals(1, root.get(0).asInt());
      assertEquals(2, root.get(1).asInt());
    }

    @Test
    void strayColon() {
      JsonParser p = parser("[1 : 2]");
      JsonNode root = p.parse();
      assertEquals(2, root.size());
      assertEquals(1, root.get(0).asInt());
      assertEquals(2, root.get(1).asInt());
    }

    @Test
    void strayClosingBrace() {
      JsonParser p = parser("[1 } 2]");
      JsonNode root = p.parse();
      assertEquals(2, root.size());
      assertEquals(1, root.get(0).asInt());
      assertEquals(2, root.get(1).asInt());
    }

    @Test
    void allStrayCommas() {
      JsonParser p = parser("[,,,]");
      JsonNode root = p.parse();
      assertEquals(0, root.size());
    }

    @Test
    void strayColonBetweenStrings() {
      JsonParser p = parser("[\"a\" : \"b\"]");
      JsonNode root = p.parse();
      assertEquals(2, root.size());
      assertEquals("a", root.get(0).asText());
      assertEquals("b", root.get(1).asText());
    }
  }

  @Nested
  class ParseStringTests {
    @Test
    void parseWithString() {
      JsonParser p = new JsonParser(new char[1024], new CharBuf(64), new NameTable());
      JsonNode root = p.parse("{\"x\": 42}");
      assertEquals(42, root.get("x").asInt());
    }

    @Test
    void parseMultipleTimes() {
      JsonParser p = new JsonParser(new char[1024], new CharBuf(64), new NameTable());

      JsonNode r1 = p.parse("{\"a\": 1}");
      assertEquals(1, r1.get("a").asInt());

      JsonNode r2 = p.parse("[10, 20]");
      assertInstanceOf(ArrayNode.class, r2);
      assertEquals(10, r2.get(0).asInt());
    }
  }
}
