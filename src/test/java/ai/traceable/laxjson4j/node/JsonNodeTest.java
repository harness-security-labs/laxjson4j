package ai.traceable.laxjson4j.node;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JsonNodeTest {

  // ── NodeType ────────────────────────────────────────────────────────

  @Test
  void nodeTypeEnumHasTenValues() {
    assertEquals(10, NodeType.values().length);
  }

  // ── NullNode ────────────────────────────────────────────────────────

  @Nested
  class NullNodeTests {
    @Test
    void singletonIdentity() {
      assertSame(NullNode.INSTANCE, NullNode.INSTANCE);
    }

    @Test
    void typeAndChecks() {
      assertEquals(NodeType.NULL, NullNode.INSTANCE.type());
      assertTrue(NullNode.INSTANCE.isNull());
      assertFalse(NullNode.INSTANCE.isObject());
      assertFalse(NullNode.INSTANCE.isArray());
      assertFalse(NullNode.INSTANCE.isNumber());
      assertFalse(NullNode.INSTANCE.isTextual());
      assertFalse(NullNode.INSTANCE.isBoolean());
    }

    @Test
    void accessors() {
      assertEquals("null", NullNode.INSTANCE.asText());
      assertEquals(0, NullNode.INSTANCE.asInt());
      assertEquals(0L, NullNode.INSTANCE.asLong());
      assertEquals(0.0, NullNode.INSTANCE.asDouble());
      assertNull(NullNode.INSTANCE.asBigInt());
      assertNull(NullNode.INSTANCE.asBigDecimal());
      assertNull(NullNode.INSTANCE.asNumber());
      assertFalse(NullNode.INSTANCE.asBoolean());
    }

    @Test
    void toStringIsNull() {
      assertEquals("null", NullNode.INSTANCE.toString());
    }
  }

  // ── BooleanNode ─────────────────────────────────────────────────────

  @Nested
  class BooleanNodeTests {
    @Test
    void singletonIdentity() {
      assertSame(BooleanNode.TRUE, BooleanNode.valueOf(true));
      assertSame(BooleanNode.FALSE, BooleanNode.valueOf(false));
      assertNotSame(BooleanNode.TRUE, BooleanNode.FALSE);
    }

    @Test
    void typeAndChecks() {
      assertEquals(NodeType.BOOLEAN, BooleanNode.TRUE.type());
      assertTrue(BooleanNode.TRUE.isBoolean());
      assertFalse(BooleanNode.TRUE.isNull());
      assertFalse(BooleanNode.TRUE.isNumber());
      assertFalse(BooleanNode.TRUE.isTextual());
    }

    @Test
    void accessors() {
      assertTrue(BooleanNode.TRUE.asBoolean());
      assertFalse(BooleanNode.FALSE.asBoolean());
      assertEquals("true", BooleanNode.TRUE.asText());
      assertEquals("false", BooleanNode.FALSE.asText());
      assertEquals(1, BooleanNode.TRUE.asInt());
      assertEquals(0, BooleanNode.FALSE.asInt());
      assertEquals(1L, BooleanNode.TRUE.asLong());
      assertEquals(0L, BooleanNode.FALSE.asLong());
      assertEquals(1.0, BooleanNode.TRUE.asDouble());
      assertEquals(0.0, BooleanNode.FALSE.asDouble());
      assertNull(BooleanNode.TRUE.asNumber());
    }

    @Test
    void toStringValues() {
      assertEquals("true", BooleanNode.TRUE.toString());
      assertEquals("false", BooleanNode.FALSE.toString());
    }
  }

  // ── IntNode ─────────────────────────────────────────────────────────

  @Nested
  class IntNodeTests {
    @Test
    void cacheHitsForNeg1To10() {
      for (int i = -1; i <= 10; i++) {
        assertSame(IntNode.valueOf(i), IntNode.valueOf(i), "cache miss for " + i);
      }
    }

    @Test
    void cacheMissOutsideRange() {
      assertNotSame(IntNode.valueOf(-2), IntNode.valueOf(-2));
      assertNotSame(IntNode.valueOf(11), IntNode.valueOf(11));
      assertNotSame(IntNode.valueOf(100), IntNode.valueOf(100));
    }

    @Test
    void typeAndChecks() {
      IntNode n = IntNode.valueOf(42);
      assertEquals(NodeType.INT, n.type());
      assertTrue(n.isNumber());
      assertFalse(n.isObject());
      assertFalse(n.isTextual());
      assertFalse(n.isBoolean());
      assertFalse(n.isNull());
    }

    @Test
    void accessors() {
      IntNode n = IntNode.valueOf(42);
      assertEquals(42, n.asInt());
      assertEquals(42L, n.asLong());
      assertEquals(42.0, n.asDouble());
      assertEquals(42, n.asNumber());
      assertEquals(BigInteger.valueOf(42), n.asBigInt());
      assertEquals("42", n.asText());
      assertTrue(n.asBoolean());
    }

    @Test
    void zeroIntBooleanIsFalse() {
      assertFalse(IntNode.valueOf(0).asBoolean());
    }

    @Test
    void negativeValue() {
      IntNode n = IntNode.valueOf(-99);
      assertEquals(-99, n.asInt());
      assertEquals("-99", n.asText());
      assertEquals("-99", n.toString());
    }

    @Test
    void toStringValue() {
      assertEquals("0", IntNode.valueOf(0).toString());
      assertEquals("42", IntNode.valueOf(42).toString());
      assertEquals("-1", IntNode.valueOf(-1).toString());
    }

    @Test
    void defaultContainerAccessors() {
      IntNode n = IntNode.valueOf(5);
      assertNull(n.get("key"));
      assertNull(n.get(0));
      assertEquals(0, n.size());
    }
  }

  // ── StringNode ──────────────────────────────────────────────────────

  @Nested
  class StringNodeTests {
    @Test
    void typeAndChecks() {
      StringNode s = new StringNode("hello");
      assertEquals(NodeType.STRING, s.type());
      assertTrue(s.isTextual());
      assertFalse(s.isNumber());
      assertFalse(s.isObject());
    }

    @Test
    void asTextReturnsValue() {
      assertEquals("hello", new StringNode("hello").asText());
      assertEquals("", new StringNode("").asText());
    }

    @Test
    void defaultNumericAccessors() {
      StringNode s = new StringNode("hello");
      assertEquals(0, s.asInt());
      assertEquals(0L, s.asLong());
      assertEquals(0.0, s.asDouble());
      assertNull(s.asNumber());
      assertFalse(s.asBoolean());
    }

    @Test
    void toStringEscapesPlain() {
      assertEquals("\"hello\"", new StringNode("hello").toString());
    }

    @Test
    void toStringEscapesQuotes() {
      assertEquals("\"say \\\"hi\\\"\"", new StringNode("say \"hi\"").toString());
    }

    @Test
    void toStringEscapesBackslash() {
      assertEquals("\"a\\\\b\"", new StringNode("a\\b").toString());
    }

    @Test
    void toStringEscapesControlChars() {
      assertEquals("\"a\\nb\"", new StringNode("a\nb").toString());
      assertEquals("\"a\\rb\"", new StringNode("a\rb").toString());
      assertEquals("\"a\\tb\"", new StringNode("a\tb").toString());
      assertEquals("\"a\\bb\"", new StringNode("a\bb").toString());
      assertEquals("\"a\\fb\"", new StringNode("a\fb").toString());
    }

    @Test
    void toStringEscapesLowAscii() {
      assertEquals("\"\\u0000\"", new StringNode("\u0000").toString());
      assertEquals("\"\\u001f\"", new StringNode("\u001f").toString());
      assertEquals("\"\\u0001x\"", new StringNode("\u0001x").toString());
    }

    @Test
    void toStringPassesThroughUnicode() {
      assertEquals("\"\u00e9\"", new StringNode("\u00e9").toString());
      assertEquals("\"\u4e16\u754c\"", new StringNode("\u4e16\u754c").toString());
    }

    @Test
    void toStringEmpty() {
      assertEquals("\"\"", new StringNode("").toString());
    }
  }

  // ── LongNode ────────────────────────────────────────────────────────

  @Nested
  class LongNodeTests {
    @Test
    void typeAndChecks() {
      LongNode n = new LongNode(100L);
      assertEquals(NodeType.LONG, n.type());
      assertTrue(n.isNumber());
      assertFalse(n.isTextual());
    }

    @Test
    void accessors() {
      LongNode n = new LongNode(9_999_999_999L);
      assertEquals(9_999_999_999L, n.asLong());
      assertEquals((int) 9_999_999_999L, n.asInt());
      assertEquals(9_999_999_999.0, n.asDouble());
      assertEquals(9_999_999_999L, n.asNumber());
      assertEquals(BigInteger.valueOf(9_999_999_999L), n.asBigInt());
      assertEquals("9999999999", n.asText());
    }

    @Test
    void narrowingFromMaxLong() {
      LongNode n = new LongNode(Long.MAX_VALUE);
      assertEquals((int) Long.MAX_VALUE, n.asInt());
    }

    @Test
    void toStringValue() {
      assertEquals("9999999999", new LongNode(9_999_999_999L).toString());
      assertEquals("-1", new LongNode(-1L).toString());
    }
  }

  // ── DoubleNode ──────────────────────────────────────────────────────

  @Nested
  class DoubleNodeTests {
    @Test
    void typeAndChecks() {
      DoubleNode n = new DoubleNode(3.14);
      assertEquals(NodeType.DOUBLE, n.type());
      assertTrue(n.isNumber());
    }

    @Test
    void accessors() {
      DoubleNode n = new DoubleNode(3.14);
      assertEquals(3.14, n.asDouble());
      assertEquals(3, n.asInt());
      assertEquals(3L, n.asLong());
      assertEquals(3.14, n.asNumber());
      assertEquals("3.14", n.asText());
    }

    @Test
    void toStringValue() {
      assertEquals("3.14", new DoubleNode(3.14).toString());
      assertEquals("0.0", new DoubleNode(0.0).toString());
      assertEquals("-1.5", new DoubleNode(-1.5).toString());
    }
  }

  // ── BigIntegerNode ──────────────────────────────────────────────────

  @Nested
  class BigIntegerNodeTests {
    private final BigInteger BIG = new BigInteger("99999999999999999999");

    @Test
    void typeAndChecks() {
      BigIntegerNode n = new BigIntegerNode(BIG);
      assertEquals(NodeType.BIG_INTEGER, n.type());
      assertTrue(n.isNumber());
      assertFalse(n.isTextual());
    }

    @Test
    void accessors() {
      BigIntegerNode n = new BigIntegerNode(BIG);
      assertEquals(BIG, n.asBigInt());
      assertEquals(BIG.longValue(), n.asLong());
      assertEquals(BIG.intValue(), n.asInt());
      assertEquals(BIG.doubleValue(), n.asDouble());
      assertSame(BIG, n.asNumber());
      assertEquals("99999999999999999999", n.asText());
    }

    @Test
    void toStringValue() {
      assertEquals("99999999999999999999", new BigIntegerNode(BIG).toString());
    }
  }

  // ── BigDecimalNode ──────────────────────────────────────────────────

  @Nested
  class BigDecimalNodeTests {
    @Test
    void typeAndChecks() {
      BigDecimalNode n = new BigDecimalNode("1.23456789012345678901234567890");
      assertEquals(NodeType.BIG_DECIMAL, n.type());
      assertTrue(n.isNumber());
    }

    @Test
    void asTextReturnsRawText() {
      String raw = "1.23456789012345678901234567890";
      assertEquals(raw, new BigDecimalNode(raw).asText());
    }

    @Test
    void lazyParsing() {
      String raw = "1.5";
      BigDecimalNode n = new BigDecimalNode(raw);
      BigDecimal bd = n.asBigDecimal();
      assertNotNull(bd);
      assertEquals(new BigDecimal("1.5"), bd);
      assertSame(bd, n.asBigDecimal(), "should cache parsed value");
    }

    @Test
    void asDoubleFromRaw() {
      assertEquals(3.14, new BigDecimalNode("3.14").asDouble(), 0.001);
    }

    @Test
    void asNumberReturnsBigDecimal() {
      BigDecimalNode n = new BigDecimalNode("2.718");
      assertInstanceOf(BigDecimal.class, n.asNumber());
    }

    @Test
    void toStringReturnsRawText() {
      String raw = "1.23456789012345678901234567890";
      assertEquals(raw, new BigDecimalNode(raw).toString());
    }
  }

  // ── ObjectNode ──────────────────────────────────────────────────────

  @Nested
  class ObjectNodeTests {
    @Test
    void typeAndChecks() {
      ObjectNode obj = new ObjectNode();
      assertEquals(NodeType.OBJECT, obj.type());
      assertTrue(obj.isObject());
      assertFalse(obj.isArray());
      assertFalse(obj.isNumber());
    }

    @Test
    void putAndGet() {
      ObjectNode obj = new ObjectNode();
      obj.put("name", new StringNode("alice"));
      obj.put("age", IntNode.valueOf(30));

      assertEquals("alice", obj.get("name").asText());
      assertEquals(30, obj.get("age").asInt());
      assertNull(obj.get("missing"));
    }

    @Test
    void sizeAndKeySet() {
      ObjectNode obj = new ObjectNode();
      assertEquals(0, obj.size());

      obj.put("a", IntNode.valueOf(1));
      obj.put("b", IntNode.valueOf(2));
      assertEquals(2, obj.size());
      assertEquals(Set.of("a", "b"), obj.keySet());
    }

    @Test
    void duplicateKeyMergesIntoArray() {
      ObjectNode obj = new ObjectNode();
      obj.put("k", IntNode.valueOf(1));
      obj.put("k", IntNode.valueOf(2));

      JsonNode merged = obj.get("k");
      assertInstanceOf(ArrayNode.class, merged);
      ArrayNode arr = (ArrayNode) merged;
      assertEquals(2, arr.size());
      assertEquals(1, arr.get(0).asInt());
      assertEquals(2, arr.get(1).asInt());
    }

    @Test
    void duplicateKeyThirdValueAppendsToArray() {
      ObjectNode obj = new ObjectNode();
      obj.put("k", new StringNode("a"));
      obj.put("k", new StringNode("b"));
      obj.put("k", new StringNode("c"));

      ArrayNode arr = (ArrayNode) obj.get("k");
      assertEquals(3, arr.size());
      assertEquals("a", arr.get(0).asText());
      assertEquals("b", arr.get(1).asText());
      assertEquals("c", arr.get(2).asText());
    }

    @Test
    void fieldsIterator() {
      ObjectNode obj = new ObjectNode();
      obj.put("x", IntNode.valueOf(1));
      obj.put("y", IntNode.valueOf(2));

      Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
      assertTrue(it.hasNext());
      Map.Entry<String, JsonNode> e1 = it.next();
      assertEquals("x", e1.getKey());
      assertEquals(1, e1.getValue().asInt());

      assertTrue(it.hasNext());
      Map.Entry<String, JsonNode> e2 = it.next();
      assertEquals("y", e2.getKey());
      assertFalse(it.hasNext());
    }

    @Test
    void stream() {
      ObjectNode obj = new ObjectNode();
      obj.put("a", IntNode.valueOf(1));
      obj.put("b", IntNode.valueOf(2));

      List<String> keys = obj.stream().map(Map.Entry::getKey).collect(Collectors.toList());
      assertEquals(List.of("a", "b"), keys);
    }

    @Test
    void toStringEmpty() {
      assertEquals("{}", new ObjectNode().toString());
    }

    @Test
    void toStringSimple() {
      ObjectNode obj = new ObjectNode();
      obj.put("name", new StringNode("alice"));
      obj.put("age", IntNode.valueOf(30));
      assertEquals("{\"name\":\"alice\",\"age\":30}", obj.toString());
    }

    @Test
    void toStringEscapesKeys() {
      ObjectNode obj = new ObjectNode();
      obj.put("key\"with\"quotes", IntNode.valueOf(1));
      assertEquals("{\"key\\\"with\\\"quotes\":1}", obj.toString());
    }

    @Test
    void defaultValueAccessors() {
      ObjectNode obj = new ObjectNode();
      assertNull(obj.asText());
      assertEquals(0, obj.asInt());
      assertFalse(obj.asBoolean());
    }

    @Test
    void putNullKeyThrowsInFlatMode() {
      ObjectNode obj = new ObjectNode();
      assertThrows(NullPointerException.class, () -> obj.put(null, IntNode.valueOf(1)));
    }

    @Test
    void putNullKeyThrowsInOverflowMode() {
      ObjectNode obj = new ObjectNode();
      for (int i = 0; i < 9; i++) obj.put("k" + i, IntNode.valueOf(i));
      assertThrows(NullPointerException.class, () -> obj.put(null, IntNode.valueOf(99)));
    }
  }

  // ── ObjectNode storage-mode promotion ──────────────────────────────

  @Nested
  class ObjectNodeStorageModeTests {

    /** Build an ObjectNode with exactly `count` distinct keys k0..k(n-1). */
    private ObjectNode buildWithKeys(int count) {
      ObjectNode obj = new ObjectNode();
      for (int i = 0; i < count; i++) {
        obj.put("k" + i, IntNode.valueOf(i));
      }
      return obj;
    }

    // ── boundary: stays flat at capacity ──────────────────────────────

    @Test
    void exactlyFlatCapacityStaysInFlatMode() {
      // 8 keys — must not promote; all accessors must work
      ObjectNode obj = buildWithKeys(8);
      assertEquals(8, obj.size());
      for (int i = 0; i < 8; i++) {
        assertEquals(i, obj.get("k" + i).asInt(), "get k" + i);
      }
    }

    @Test
    void flatCapacityInsertionOrderPreserved() {
      ObjectNode obj = buildWithKeys(8);
      List<String> keys = obj.stream().map(Map.Entry::getKey).collect(Collectors.toList());
      assertEquals(List.of("k0", "k1", "k2", "k3", "k4", "k5", "k6", "k7"), keys);
    }

    // ── boundary: 9th key triggers promotion ──────────────────────────

    @Test
    void ninthKeyTriggersPromotion() {
      ObjectNode obj = buildWithKeys(9);
      assertEquals(9, obj.size());
    }

    @Test
    void getWorksForAllKeysAfterPromotion() {
      ObjectNode obj = buildWithKeys(9);
      for (int i = 0; i < 9; i++) {
        assertNotNull(obj.get("k" + i), "key k" + i + " should exist");
        assertEquals(i, obj.get("k" + i).asInt(), "value for k" + i);
      }
      assertNull(obj.get("missing"));
    }

    @Test
    void insertionOrderPreservedAfterPromotion() {
      ObjectNode obj = buildWithKeys(9);
      List<String> keys = obj.stream().map(Map.Entry::getKey).collect(Collectors.toList());
      assertEquals(
          List.of("k0", "k1", "k2", "k3", "k4", "k5", "k6", "k7", "k8"),
          keys,
          "LinkedHashMap must preserve insertion order");
    }

    @Test
    void fieldsIteratorWorksAfterPromotion() {
      ObjectNode obj = buildWithKeys(9);
      Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
      int count = 0;
      while (it.hasNext()) {
        Map.Entry<String, JsonNode> e = it.next();
        assertEquals("k" + count, e.getKey());
        assertEquals(count, e.getValue().asInt());
        count++;
      }
      assertEquals(9, count);
    }

    @Test
    void keySetAfterPromotion() {
      ObjectNode obj = buildWithKeys(9);
      Set<String> expected =
          new java.util.LinkedHashSet<>(
              List.of("k0", "k1", "k2", "k3", "k4", "k5", "k6", "k7", "k8"));
      assertEquals(expected, obj.keySet());
    }

    @Test
    void toStringAfterPromotion() {
      ObjectNode obj = new ObjectNode();
      obj.put("a", new StringNode("x"));
      obj.put("b", IntNode.valueOf(1));
      obj.put("c", BooleanNode.TRUE);
      obj.put("d", NullNode.INSTANCE);
      obj.put("e", IntNode.valueOf(5));
      obj.put("f", IntNode.valueOf(6));
      obj.put("g", IntNode.valueOf(7));
      obj.put("h", IntNode.valueOf(8));
      obj.put("i", IntNode.valueOf(9)); // 9th key → promotes
      assertEquals(
          "{\"a\":\"x\",\"b\":1,\"c\":true,\"d\":null,"
              + "\"e\":5,\"f\":6,\"g\":7,\"h\":8,\"i\":9}",
          obj.toString());
    }

    @Test
    void writeToAfterPromotion() {
      ObjectNode obj = buildWithKeys(9);
      StringBuilder sb = new StringBuilder();
      obj.writeTo(sb);
      String json = sb.toString();
      assertTrue(json.startsWith("{"), "must open with {");
      assertTrue(json.endsWith("}"), "must close with }");
      for (int i = 0; i < 9; i++) {
        assertTrue(json.contains("\"k" + i + "\":" + i), "must contain k" + i);
      }
    }

    @Test
    void continuedInsertsAfterPromotionWork() {
      ObjectNode obj = buildWithKeys(9); // overflow now active
      obj.put("extra", new StringNode("yes"));
      assertEquals(10, obj.size());
      assertEquals("yes", obj.get("extra").asText());
    }

    // ── duplicate-key merging in overflow mode ─────────────────────────

    @Test
    void duplicateKeyMergesIntoArrayAfterPromotion() {
      ObjectNode obj = buildWithKeys(9); // forces overflow
      obj.put("k0", IntNode.valueOf(99)); // duplicate of first key
      JsonNode merged = obj.get("k0");
      assertInstanceOf(
          ArrayNode.class, merged, "duplicate in overflow mode must be collapsed into ArrayNode");
      ArrayNode arr = (ArrayNode) merged;
      assertEquals(2, arr.size());
      assertEquals(0, arr.get(0).asInt());
      assertEquals(99, arr.get(1).asInt());
    }

    @Test
    void duplicateKeyThirdValueAppendsAfterPromotion() {
      ObjectNode obj = buildWithKeys(9); // forces overflow
      obj.put("k1", new StringNode("second"));
      obj.put("k1", new StringNode("third"));
      ArrayNode arr = (ArrayNode) obj.get("k1");
      assertEquals(3, arr.size());
      assertEquals("1", arr.get(0).asText()); // original IntNode(1)
      assertEquals("second", arr.get(1).asText());
      assertEquals("third", arr.get(2).asText());
    }

    @Test
    void sizeNotInflatedByDuplicatesAfterPromotion() {
      // Duplicates collapse into ArrayNode under the same key — size stays 9
      ObjectNode obj = buildWithKeys(9);
      obj.put("k0", IntNode.valueOf(100));
      obj.put("k0", IntNode.valueOf(200));
      assertEquals(9, obj.size(), "duplicate puts must not grow size()");
    }

    // ── duplicate handling spanning both modes ─────────────────────────

    @Test
    void duplicateFirstAddedInFlatSurvivesPromotion() {
      ObjectNode obj = new ObjectNode();
      // Add duplicate while still in flat mode
      obj.put("dup", IntNode.valueOf(1));
      obj.put("dup", IntNode.valueOf(2));
      // Now push past FLAT_CAPACITY to trigger promotion
      for (int i = 0; i < 8; i++) {
        obj.put("x" + i, IntNode.valueOf(i));
      }
      // After promotion the merged ArrayNode must still be intact
      JsonNode merged = obj.get("dup");
      assertInstanceOf(ArrayNode.class, merged);
      ArrayNode arr = (ArrayNode) merged;
      assertEquals(2, arr.size());
      assertEquals(1, arr.get(0).asInt());
      assertEquals(2, arr.get(1).asInt());
    }
  }

  // ── ArrayNode ───────────────────────────────────────────────────────

  @Nested
  class ArrayNodeTests {
    @Test
    void typeAndChecks() {
      ArrayNode arr = new ArrayNode();
      assertEquals(NodeType.ARRAY, arr.type());
      assertTrue(arr.isArray());
      assertFalse(arr.isObject());
      assertFalse(arr.isNumber());
    }

    @Test
    void addAndGet() {
      ArrayNode arr = new ArrayNode();
      arr.add(IntNode.valueOf(10));
      arr.add(new StringNode("hi"));

      assertEquals(10, arr.get(0).asInt());
      assertEquals("hi", arr.get(1).asText());
    }

    @Test
    void boundsSafeGetReturnsNull() {
      ArrayNode arr = new ArrayNode();
      arr.add(IntNode.valueOf(1));

      assertNull(arr.get(-1));
      assertNull(arr.get(1));
      assertNull(arr.get(100));
    }

    @Test
    void size() {
      ArrayNode arr = new ArrayNode();
      assertEquals(0, arr.size());
      arr.add(NullNode.INSTANCE);
      arr.add(NullNode.INSTANCE);
      assertEquals(2, arr.size());
    }

    @Test
    void elementsIterator() {
      ArrayNode arr = new ArrayNode();
      arr.add(IntNode.valueOf(1));
      arr.add(IntNode.valueOf(2));

      Iterator<JsonNode> it = arr.elements();
      assertTrue(it.hasNext());
      assertEquals(1, it.next().asInt());
      assertTrue(it.hasNext());
      assertEquals(2, it.next().asInt());
      assertFalse(it.hasNext());
    }

    @Test
    void stream() {
      ArrayNode arr = new ArrayNode();
      arr.add(IntNode.valueOf(10));
      arr.add(IntNode.valueOf(20));

      List<Integer> ints = arr.stream().map(JsonNode::asInt).collect(Collectors.toList());
      assertEquals(List.of(10, 20), ints);
    }

    @Test
    void toStringEmpty() {
      assertEquals("[]", new ArrayNode().toString());
    }

    @Test
    void toStringMixed() {
      ArrayNode arr = new ArrayNode();
      arr.add(IntNode.valueOf(1));
      arr.add(new StringNode("two"));
      arr.add(BooleanNode.TRUE);
      arr.add(NullNode.INSTANCE);
      assertEquals("[1,\"two\",true,null]", arr.toString());
    }

    @Test
    void getByStringKeyReturnsNull() {
      ArrayNode arr = new ArrayNode();
      arr.add(IntNode.valueOf(1));
      assertNull(arr.get("key"));
    }
  }

  // ── Nested structures ──────────────────────────────────────────────

  @Nested
  class NestedStructureTests {
    @Test
    void objectContainingArrayContainingMixedTypes() {
      ObjectNode root = new ObjectNode();
      root.put("name", new StringNode("test"));

      ArrayNode items = new ArrayNode();
      items.add(IntNode.valueOf(1));
      items.add(new DoubleNode(2.5));
      items.add(BooleanNode.FALSE);
      items.add(NullNode.INSTANCE);

      ObjectNode nested = new ObjectNode();
      nested.put("inner", new StringNode("value"));
      items.add(nested);

      root.put("items", items);
      root.put("active", BooleanNode.TRUE);

      String expected =
          "{\"name\":\"test\","
              + "\"items\":[1,2.5,false,null,{\"inner\":\"value\"}],"
              + "\"active\":true}";
      assertEquals(expected, root.toString());
    }

    @Test
    void deeplyNestedObject() {
      ObjectNode level3 = new ObjectNode();
      level3.put("deep", new StringNode("value"));

      ObjectNode level2 = new ObjectNode();
      level2.put("child", level3);

      ObjectNode level1 = new ObjectNode();
      level1.put("parent", level2);

      assertEquals("{\"parent\":{\"child\":{\"deep\":\"value\"}}}", level1.toString());
    }

    @Test
    void arrayOfArrays() {
      ArrayNode inner1 = new ArrayNode();
      inner1.add(IntNode.valueOf(1));
      inner1.add(IntNode.valueOf(2));

      ArrayNode inner2 = new ArrayNode();
      inner2.add(IntNode.valueOf(3));

      ArrayNode outer = new ArrayNode();
      outer.add(inner1);
      outer.add(inner2);

      assertEquals("[[1,2],[3]]", outer.toString());
    }

    @Test
    void allNumericTypes() {
      ArrayNode arr = new ArrayNode();
      arr.add(IntNode.valueOf(42));
      arr.add(new LongNode(9_999_999_999L));
      arr.add(new DoubleNode(3.14));
      arr.add(new BigIntegerNode(new BigInteger("99999999999999999999")));
      arr.add(new BigDecimalNode("1.234567890123456789"));

      assertEquals(
          "[42,9999999999,3.14,99999999999999999999,1.234567890123456789]", arr.toString());
    }
  }

  // ── Cross-type default accessor behaviour ──────────────────────────

  @Nested
  class DefaultAccessorTests {
    @Test
    void leafNodesReturnEmptyContainerDefaults() {
      JsonNode[] leaves = {
        IntNode.valueOf(1),
        new LongNode(1L),
        new DoubleNode(1.0),
        new StringNode("s"),
        BooleanNode.TRUE,
        NullNode.INSTANCE,
        new BigIntegerNode(BigInteger.ONE),
        new BigDecimalNode("1.0")
      };
      for (JsonNode node : leaves) {
        assertNull(node.get("key"), node.type() + ".get(String)");
        assertNull(node.get(0), node.type() + ".get(int)");
        assertEquals(0, node.size(), node.type() + ".size()");
        assertFalse(node.elements().hasNext(), node.type() + ".elements()");
        assertFalse(node.fields().hasNext(), node.type() + ".fields()");
      }
    }

    @Test
    void containerNodesReturnDefaultValueAccessors() {
      ObjectNode obj = new ObjectNode();
      assertNull(obj.asText());
      assertEquals(0, obj.asInt());
      assertEquals(0L, obj.asLong());
      assertEquals(0.0, obj.asDouble());
      assertNull(obj.asBigInt());
      assertNull(obj.asNumber());
      assertFalse(obj.asBoolean());

      ArrayNode arr = new ArrayNode();
      assertNull(arr.asText());
      assertEquals(0, arr.asInt());
      assertFalse(arr.asBoolean());
    }
  }

  // ── Cross-type coercion ──────────────────────────────────────────────

  @Nested
  class CrossTypeAccessorTests {

    @Test
    void booleanFromNegativeInt() {
      assertTrue(IntNode.valueOf(-5).asBoolean());
    }

    @Test
    void booleanFromNonZeroLong() {
      assertTrue(new LongNode(999999999999L).asBoolean());
    }

    @Test
    void booleanFromZeroLong() {
      assertFalse(new LongNode(0L).asBoolean());
    }

    @Test
    void booleanFromNonZeroDouble() {
      assertTrue(new DoubleNode(0.001).asBoolean());
    }

    @Test
    void booleanFromZeroDouble() {
      assertFalse(new DoubleNode(0.0).asBoolean());
    }

    @Test
    void booleanFromNonZeroBigInteger() {
      assertTrue(new BigIntegerNode(BigInteger.TEN).asBoolean());
    }

    @Test
    void booleanFromZeroBigInteger() {
      assertFalse(new BigIntegerNode(BigInteger.ZERO).asBoolean());
    }

    @Test
    void booleanFromNonZeroBigDecimal() {
      assertTrue(new BigDecimalNode("3.14").asBoolean());
    }

    @Test
    void booleanFromZeroBigDecimal() {
      assertFalse(new BigDecimalNode("0").asBoolean());
    }

    @Test
    void booleanFromStringTrue() {
      assertTrue(new StringNode("true").asBoolean());
    }

    @Test
    void booleanFromStringTrueCaseInsensitive() {
      assertTrue(new StringNode("TRUE").asBoolean());
      assertTrue(new StringNode("True").asBoolean());
    }

    @Test
    void booleanFromStringFalse() {
      assertFalse(new StringNode("false").asBoolean());
    }

    @Test
    void booleanFromEmptyString() {
      assertFalse(new StringNode("").asBoolean());
    }

    @Test
    void bigDecimalFromBigInteger() {
      JsonNode node = new BigIntegerNode(BigInteger.valueOf(42));
      assertNotNull(node.asBigDecimal());
      assertEquals(new BigDecimal("42"), node.asBigDecimal());
    }

    @Test
    void intAndLongFromBigDecimal() {
      JsonNode node = new BigDecimalNode("12345.67");
      assertEquals(12345, node.asInt());
      assertEquals(12345L, node.asLong());
    }
  }

  @Nested
  class ArrayNodeSetTests {

    private ArrayNode of(int... values) {
      ArrayNode arr = new ArrayNode();
      for (int v : values) arr.add(IntNode.valueOf(v));
      return arr;
    }

    @Test
    void setMiddleReturnsOldValueAndReplaces() {
      ArrayNode arr = of(1, 2, 3);
      JsonNode old = arr.set(1, new StringNode("x"));
      assertEquals(2, old.asInt());
      assertEquals("[1,\"x\",3]", arr.toString());
    }

    @Test
    void setFirstElement() {
      ArrayNode arr = of(1, 2, 3);
      JsonNode old = arr.set(0, new StringNode("a"));
      assertEquals(1, old.asInt());
      assertEquals("[\"a\",2,3]", arr.toString());
    }

    @Test
    void setLastElement() {
      ArrayNode arr = of(1, 2, 3);
      JsonNode old = arr.set(2, new StringNode("z"));
      assertEquals(3, old.asInt());
      assertEquals("[1,2,\"z\"]", arr.toString());
    }

    @Test
    void setDoesNotChangeSize() {
      ArrayNode arr = of(1, 2, 3);
      arr.set(1, NullNode.INSTANCE);
      assertEquals(3, arr.size());
    }

    @Test
    void setNegativeIndexThrows() {
      ArrayNode arr = of(1, 2, 3);
      assertThrows(IndexOutOfBoundsException.class, () -> arr.set(-1, NullNode.INSTANCE));
    }

    @Test
    void setIndexAtSizeThrows() {
      ArrayNode arr = of(1, 2, 3);
      assertThrows(IndexOutOfBoundsException.class, () -> arr.set(3, NullNode.INSTANCE));
    }

    @Test
    void setOnEmptyArrayThrows() {
      assertThrows(
          IndexOutOfBoundsException.class, () -> new ArrayNode().set(0, NullNode.INSTANCE));
    }

    @Test
    void setNullValueThrows() {
      ArrayNode arr = of(1, 2, 3);
      assertThrows(NullPointerException.class, () -> arr.set(0, null));
    }
  }

  @Nested
  class IsEmptyTests {

    @Test
    void emptyObjectIsEmpty() {
      assertTrue(new ObjectNode().isEmpty());
    }

    @Test
    void emptyArrayIsEmpty() {
      assertTrue(new ArrayNode().isEmpty());
    }

    @Test
    void nonEmptyObjectIsNotEmpty() {
      ObjectNode obj = new ObjectNode();
      obj.put("a", IntNode.valueOf(1));
      assertFalse(obj.isEmpty());
    }

    @Test
    void nonEmptyArrayIsNotEmpty() {
      ArrayNode arr = new ArrayNode();
      arr.add(IntNode.valueOf(1));
      assertFalse(arr.isEmpty());
    }

    @Test
    void scalarsAreEmptyJacksonParity() {
      assertTrue(IntNode.valueOf(1).isEmpty());
      assertTrue(new StringNode("x").isEmpty());
      assertTrue(BooleanNode.TRUE.isEmpty());
      assertTrue(NullNode.INSTANCE.isEmpty());
    }

    @Test
    void promotedObjectIsNotEmpty() {
      ObjectNode obj = new ObjectNode();
      for (int i = 0; i < 9; i++) obj.put("k" + i, IntNode.valueOf(i));
      assertFalse(obj.isEmpty());
    }
  }

  @Nested
  class IsValueNodeTests {

    @Test
    void containersAreNotValueNodes() {
      assertFalse(new ObjectNode().isValueNode());
      assertFalse(new ArrayNode().isValueNode());
    }

    @Test
    void scalarsAreValueNodes() {
      assertTrue(IntNode.valueOf(1).isValueNode());
      assertTrue(new LongNode(1L).isValueNode());
      assertTrue(new DoubleNode(1.0).isValueNode());
      assertTrue(new StringNode("s").isValueNode());
      assertTrue(BooleanNode.TRUE.isValueNode());
      assertTrue(new BigIntegerNode(BigInteger.ONE).isValueNode());
      assertTrue(new BigDecimalNode("1.0").isValueNode());
    }

    @Test
    void nullNodeIsValueNodeJacksonParity() {
      assertTrue(NullNode.INSTANCE.isValueNode());
    }
  }

  @Nested
  class IterableTests {

    @Test
    void arrayEnhancedForYieldsElementsInOrder() {
      ArrayNode arr = new ArrayNode();
      arr.add(IntNode.valueOf(1));
      arr.add(IntNode.valueOf(2));
      arr.add(IntNode.valueOf(3));

      List<Integer> seen = new java.util.ArrayList<>();
      for (JsonNode n : arr) seen.add(n.asInt());
      assertEquals(List.of(1, 2, 3), seen);
    }

    @Test
    void scalarEnhancedForBodyNeverRuns() {
      for (JsonNode n : IntNode.valueOf(5)) {
        fail("scalar iteration must not yield elements: " + n);
      }
    }

    @Test
    void nullEnhancedForBodyNeverRuns() {
      for (JsonNode n : NullNode.INSTANCE) {
        fail("null iteration must not yield elements: " + n);
      }
    }

    @Test
    void objectEnhancedForYieldsValuesFlatMode() {
      ObjectNode obj = new ObjectNode();
      obj.put("a", IntNode.valueOf(1));
      obj.put("b", IntNode.valueOf(2));
      obj.put("c", IntNode.valueOf(3));

      List<Integer> values = new java.util.ArrayList<>();
      for (JsonNode v : obj) values.add(v.asInt());
      assertEquals(List.of(1, 2, 3), values);
    }

    @Test
    void objectEnhancedForYieldsValuesOverflowMode() {
      ObjectNode obj = new ObjectNode();
      for (int i = 0; i < 9; i++) obj.put("k" + i, IntNode.valueOf(i));

      List<Integer> values = new java.util.ArrayList<>();
      for (JsonNode v : obj) values.add(v.asInt());
      assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8), values);
    }

    @Test
    void objectElementsMatchesEnhancedFor() {
      ObjectNode obj = new ObjectNode();
      obj.put("x", new StringNode("one"));
      obj.put("y", new StringNode("two"));

      Iterator<JsonNode> it = obj.elements();
      assertTrue(it.hasNext());
      assertEquals("one", it.next().asText());
      assertEquals("two", it.next().asText());
      assertFalse(it.hasNext());
    }

    @Test
    void emptyContainersIterateZeroTimes() {
      for (JsonNode n : new ObjectNode()) fail("empty object should not iterate: " + n);
      for (JsonNode n : new ArrayNode()) fail("empty array should not iterate: " + n);
    }
  }
}
