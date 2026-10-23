package ai.traceable.laxjson4j;

import static org.junit.jupiter.api.Assertions.*;

import ai.traceable.laxjson4j.exception.JsonParseException;
import ai.traceable.laxjson4j.node.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JsonEntryPointTest {

  private final Json jsonParser = new Json();

  // ── Invalid input via Json.parse() API ────────────────────────────────

  @Nested
  class InvalidInputTests {
    @Test
    void nullInputThrows() {
      assertThrows(IllegalArgumentException.class, () -> jsonParser.parse(null));
    }

    @Test
    void stringRootThrows() {
      assertThrows(JsonParseException.class, () -> jsonParser.parse("\"JustAString\""));
    }

    @Test
    void booleanRootThrows() {
      assertThrows(JsonParseException.class, () -> jsonParser.parse("true"));
    }

    @Test
    void nullRootThrows() {
      assertThrows(JsonParseException.class, () -> jsonParser.parse("null"));
    }

    @Test
    void functionWrappedJsonThrows() {
      String json =
          "  testJsonFunction({\n"
              + "    \"user\": \"thisisuser\",\n"
              + "    \"age\": 23\n"
              + "  })\n";
      assertThrows(JsonParseException.class, () -> jsonParser.parse(json));
    }

    @Test
    void logLinesWithJsonThrow() {
      String json =
          "2025-04-03 12:34:56,789 INFO User login successful - "
              + "{\"timestamp\": \"2025-04-03T12:34:56Z\"}\n";
      assertThrows(JsonParseException.class, () -> jsonParser.parse(json));
    }

    @Test
    void missingColonAfterKeyThrows() {
      assertThrows(
          JsonParseException.class, () -> jsonParser.parse("{\"key1\": \"value1\", \"key2\"}"));
    }

    @Test
    void setRepresentationThrows() {
      assertThrows(
          JsonParseException.class, () -> jsonParser.parse("{ \"key1\" , \"key2\", \"key3\" }"));
    }
  }

  // ── stringify ──────────────────────────────────────────────────────

  @Nested
  class StringifyTests {
    @Test
    void stringifyObject() {
      JsonNode root = jsonParser.parse("{\"a\": 1}");
      String s = jsonParser.stringify(root);
      assertEquals("{\"a\":1}", s);
    }

    @Test
    void stringifyArray() {
      JsonNode root = jsonParser.parse("[1, true, null]");
      assertEquals("[1,true,null]", jsonParser.stringify(root));
    }

    @Test
    void stringifyComplexObject() {
      String original = "{\"name\":\"Alice\",\"scores\":[100,95,87],\"active\":true}";
      JsonNode parsed = jsonParser.parse(original);
      assertEquals(original, jsonParser.stringify(parsed));
    }
  }

  // ── Large input (> 32K) — chunked parsing via pool ─────────────────

  @Nested
  class LargeInputTests {
    @Test
    void largeObjectExceedingBufferSize() {
      StringBuilder sb = new StringBuilder("{");
      int entryCount = 5000;
      for (int i = 0; i < entryCount; i++) {
        if (i > 0) sb.append(',');
        sb.append("\"key_").append(i).append("\":").append(i);
      }
      sb.append('}');
      String json = sb.toString();
      assertTrue(json.length() > 32768, "Input must exceed 32K chars");

      JsonNode root = jsonParser.parse(json);
      assertInstanceOf(ObjectNode.class, root);
      assertEquals(entryCount, root.size());
      assertEquals(0, root.get("key_0").asInt());
      assertEquals(2500, root.get("key_2500").asInt());
      assertEquals(4999, root.get("key_4999").asInt());
    }

    @Test
    void largeArrayExceedingBufferSize() {
      StringBuilder sb = new StringBuilder("[");
      int count = 10000;
      for (int i = 0; i < count; i++) {
        if (i > 0) sb.append(',');
        sb.append(i);
      }
      sb.append(']');
      String json = sb.toString();
      assertTrue(json.length() > 32768);

      JsonNode root = jsonParser.parse(json);
      assertInstanceOf(ArrayNode.class, root);
      assertEquals(count, root.size());
      assertEquals(0, root.get(0).asInt());
      assertEquals(9999, root.get(9999).asInt());
    }

    @Test
    void largeStringValueCrossingBufferBoundary() {
      StringBuilder sb = new StringBuilder("{\"data\": \"");
      for (int i = 0; i < 40000; i++) sb.append('A');
      sb.append("\"}");
      String json = sb.toString();
      assertTrue(json.length() > 32768);

      JsonNode root = jsonParser.parse(json);
      assertEquals(40000, root.get("data").asText().length());
    }

    @Test
    void largeNestedStructure() {
      StringBuilder sb = new StringBuilder("{\"items\":[");
      int itemCount = 3000;
      for (int i = 0; i < itemCount; i++) {
        if (i > 0) sb.append(',');
        sb.append("{\"id\":")
            .append(i)
            .append(",\"name\":\"item_")
            .append(i)
            .append("\",\"active\":")
            .append(i % 2 == 0 ? "true" : "false")
            .append('}');
      }
      sb.append("]}");
      String json = sb.toString();
      assertTrue(json.length() > 32768);

      JsonNode root = jsonParser.parse(json);
      JsonNode items = root.get("items");
      assertEquals(itemCount, items.size());
      assertEquals(0, items.get(0).get("id").asInt());
      assertEquals("item_0", items.get(0).get("name").asText());
      assertTrue(items.get(0).get("active").asBoolean());
      assertFalse(items.get(1).get("active").asBoolean());
    }
  }

  // ── Sequential reuse ──────────────────────────────────────────────

  @Nested
  class ReuseTests {
    @Test
    void multipleSequentialParses() {
      for (int i = 0; i < 100; i++) {
        JsonNode root = jsonParser.parse("{\"i\": " + i + "}");
        assertEquals(i, root.get("i").asInt());
      }
    }

    @Test
    void alternatingObjectsAndArrays() {
      JsonNode obj = jsonParser.parse("{\"x\": 1}");
      assertEquals(1, obj.get("x").asInt());

      JsonNode arr = jsonParser.parse("[10, 20]");
      assertEquals(2, arr.size());

      JsonNode obj2 = jsonParser.parse("{\"y\": \"hello\"}");
      assertEquals("hello", obj2.get("y").asText());
    }
  }

  // ── Concurrency — ThreadLocal isolation ────────────────────────────

  @Nested
  class ConcurrencyTests {
    @Test
    void concurrentParsesProduceCorrectResults() throws Exception {
      int threadCount = 8;
      int parsesPerThread = 200;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      List<Future<Boolean>> futures = new ArrayList<>();

      for (int t = 0; t < threadCount; t++) {
        final int threadId = t;
        futures.add(
            executor.submit(
                () -> {
                  for (int i = 0; i < parsesPerThread; i++) {
                    String json = "{\"thread\":" + threadId + ",\"iter\":" + i + "}";
                    JsonNode root = jsonParser.parse(json);
                    if (root.get("thread").asInt() != threadId) return false;
                    if (root.get("iter").asInt() != i) return false;
                  }
                  return true;
                }));
      }

      executor.shutdown();
      assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

      for (Future<Boolean> f : futures) {
        assertTrue(f.get());
      }
    }

    @Test
    void concurrentLargeInputParses() throws Exception {
      int threadCount = 4;
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      List<Future<Boolean>> futures = new ArrayList<>();

      for (int t = 0; t < threadCount; t++) {
        final int threadId = t;
        futures.add(
            executor.submit(
                () -> {
                  StringBuilder sb = new StringBuilder("[");
                  for (int i = 0; i < 5000; i++) {
                    if (i > 0) sb.append(',');
                    sb.append(threadId * 10000 + i);
                  }
                  sb.append(']');
                  String json = sb.toString();

                  JsonNode root = jsonParser.parse(json);
                  if (root.size() != 5000) return false;
                  if (root.get(0).asInt() != threadId * 10000) return false;
                  return true;
                }));
      }

      executor.shutdown();
      assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

      for (Future<Boolean> f : futures) {
        assertTrue(f.get());
      }
    }
  }

  // ── Re-entrancy ────────────────────────────────────────────────────

  @Nested
  class ReentrancyTests {
    @Test
    void nestedParseCallsWork() {
      JsonNode outer = jsonParser.parse("{\"inner\": \"{\\\"x\\\": 1}\"}");
      String innerJson = outer.get("inner").asText();
      JsonNode inner = jsonParser.parse(innerJson);
      assertEquals(1, inner.get("x").asInt());
    }
  }
}
