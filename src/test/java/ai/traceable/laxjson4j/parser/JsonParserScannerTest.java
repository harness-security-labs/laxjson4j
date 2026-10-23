package ai.traceable.laxjson4j.parser;

import static org.junit.jupiter.api.Assertions.*;

import ai.traceable.laxjson4j.exception.JsonParseException;
import ai.traceable.laxjson4j.node.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JsonParserScannerTest {

  private static JsonParser parser(String input, int bufSize) {
    JsonParser p = new JsonParser(new char[bufSize], new CharBuf(64), new NameTable());
    p.init(input);
    return p;
  }

  private static JsonParser parser(String input) {
    return parser(input, Math.max(input.length() + 16, 64));
  }

  @Nested
  class SkipWhitespaceTests {
    @Test
    void spacesAndTabs() {
      JsonParser p = parser("   \t\t  x");
      p.skipWhitespace();
      assertEquals('x', p.buf[p.ptr]);
      assertEquals(1, p.lineNumber);
    }

    @Test
    void newlines() {
      JsonParser p = parser("\n\n\nx");
      p.skipWhitespace();
      assertEquals('x', p.buf[p.ptr]);
      assertEquals(4, p.lineNumber);
    }

    @Test
    void crlfCountsAsOneLine() {
      JsonParser p = parser("\r\n\r\nx");
      p.skipWhitespace();
      assertEquals('x', p.buf[p.ptr]);
      assertEquals(3, p.lineNumber);
    }

    @Test
    void lineComment() {
      JsonParser p = parser("// comment\nx");
      p.skipWhitespace();
      assertEquals('x', p.buf[p.ptr]);
      assertEquals(2, p.lineNumber);
    }

    @Test
    void blockCommentWithNewlines() {
      JsonParser p = parser("/* line1\nline2\nline3 */x");
      p.skipWhitespace();
      assertEquals('x', p.buf[p.ptr]);
      assertEquals(3, p.lineNumber);
    }

    @Test
    void emptyInput() {
      JsonParser p = parser("");
      p.skipWhitespace();
      assertTrue(p.ptr >= p.end);
    }

    @Test
    void allWhitespace() {
      JsonParser p = parser("   \n\t  ");
      p.skipWhitespace();
      assertTrue(p.ptr >= p.end);
    }

    @Test
    void whitespaceAcrossBufferBoundary() {
      JsonParser p = parser("          x", 8);
      p.skipWhitespace();
      assertEquals('x', p.buf[p.ptr]);
    }

    @Test
    void lineCommentAcrossBufferBoundary() {
      JsonParser p = parser("// long comment here\nx", 8);
      p.skipWhitespace();
      assertEquals('x', p.buf[p.ptr]);
      assertEquals(2, p.lineNumber);
    }

    @Test
    void blockCommentAcrossBufferBoundary() {
      JsonParser p = parser("/* block\ncomment */x", 8);
      p.skipWhitespace();
      assertEquals('x', p.buf[p.ptr]);
      assertEquals(2, p.lineNumber);
    }

    @Test
    void commentSlashAtBufferEdge() {
      JsonParser p = parser("    //comment\nx", 5);
      p.skipWhitespace();
      assertEquals('x', p.buf[p.ptr]);
      assertEquals(2, p.lineNumber);
    }

    @Test
    void slashAtBufferEdgeNotComment() {
      JsonParser p = parser("    /x", 5);
      p.skipWhitespace();
      assertEquals('/', p.buf[p.ptr]);
    }

    @Test
    void crlfAcrossBufferBoundary() {
      JsonParser p = parser("   \r\nx", 4);
      p.skipWhitespace();
      assertEquals('x', p.buf[p.ptr]);
      assertEquals(2, p.lineNumber);
    }

    @Test
    void blockCommentStarAtBufferEdge() {
      JsonParser p = parser("/* comment */x", 13);
      p.skipWhitespace();
      assertEquals('x', p.buf[p.ptr]);
    }

    @Test
    void blockCommentCrlfAcrossBufferBoundary() {
      JsonParser p = parser("/*\r\n*/x", 3);
      p.skipWhitespace();
      assertEquals('x', p.buf[p.ptr]);
      assertEquals(2, p.lineNumber);
    }

    @Test
    void blockCommentStarNotFollowedBySlash() {
      JsonParser p = parser("/* a * b */x", 6);
      p.skipWhitespace();
      assertEquals('x', p.buf[p.ptr]);
    }
  }

  @Nested
  class ParseQuotedStringTests {
    @Test
    void simpleString() {
      JsonParser p = parser("\"hello\"");
      assertEquals("hello", p.parseQuotedString());
    }

    @Test
    void emptyString() {
      JsonParser p = parser("\"\"");
      assertEquals("", p.parseQuotedString());
    }

    @Test
    void stringWithEscapedQuote() {
      JsonParser p = parser("\"he\\\"llo\"");
      assertEquals("he\"llo", p.parseQuotedString());
    }

    @Test
    void stringWithEscapedBackslash() {
      JsonParser p = parser("\"a\\\\b\"");
      assertEquals("a\\b", p.parseQuotedString());
    }

    @Test
    void stringWithEscapedNewline() {
      JsonParser p = parser("\"a\\nb\"");
      assertEquals("a\nb", p.parseQuotedString());
    }

    @Test
    void stringWithEscapedTab() {
      JsonParser p = parser("\"a\\tb\"");
      assertEquals("a\tb", p.parseQuotedString());
    }

    @Test
    void stringWithAllStandardEscapes() {
      JsonParser p = parser("\"\\b\\f\\n\\r\\t\\\\\\/\\\"\"");
      assertEquals("\b\f\n\r\t\\/\"", p.parseQuotedString());
    }

    @Test
    void stringWithUnicodeEscape() {
      JsonParser p = parser("\"abc\\u0041def\"");
      assertEquals("abcAdef", p.parseQuotedString());
    }

    @Test
    void stringTerminatedByNewline() {
      JsonParser p = parser("\"unterminated\nrest");
      assertEquals("unterminated", p.parseQuotedString());
    }

    @Test
    void stringTerminatedByEof() {
      JsonParser p = parser("\"unterminated");
      assertEquals("unterminated", p.parseQuotedString());
    }

    @Test
    void backslashBeforeNewline() {
      JsonParser p = parser("\"abc\\\ndef\"");
      assertEquals("abc\\", p.parseQuotedString());
    }

    @Test
    void stringAcrossBufferBoundary() {
      JsonParser p = parser("\"hello world test\"", 8);
      assertEquals("hello world test", p.parseQuotedString());
    }

    @Test
    void escapeAcrossBufferBoundary() {
      JsonParser p = parser("\"abcdef\\ngh\"", 8);
      assertEquals("abcdef\ngh", p.parseQuotedString());
    }

    @Test
    void unicodeEscapeAcrossBufferBoundary() {
      JsonParser p = parser("\"abcde\\u0041\"", 8);
      assertEquals("abcdeA", p.parseQuotedString());
    }

    @Test
    void unknownEscapeCharacter() {
      JsonParser p = parser("\"a\\xb\"");
      assertEquals("a\\xb", p.parseQuotedString());
    }

    @Test
    void escapedForwardSlash() {
      JsonParser p = parser("\"\\/date\"");
      assertEquals("/date", p.parseQuotedString());
    }

    @Test
    void invalidHexDigitThrows() {
      JsonParser p = parser("\"\\u00GZ\"");
      assertThrows(JsonParseException.class, p::parseQuotedString);
    }

    @Test
    void validHexDigitsAllCases() {
      JsonParser p = parser("\"\\u0041\\u006e\\u004F\"");
      assertEquals("AnO", p.parseQuotedString());
    }

    @Test
    void stringWithNonAsciiCharacters() {
      JsonParser p = parser("\"héllo wörld 日本語\"");
      assertEquals("héllo wörld 日本語", p.parseQuotedString());
    }
  }

  @Nested
  class ParseUnquotedStringTests {
    @Test
    void simpleText() {
      JsonParser p = parser("some value\n");
      assertEquals("some value", p.parseUnquotedString());
    }

    @Test
    void stripsLeadingAndTrailingWhitespace() {
      JsonParser p = parser("  hello  \n");
      assertEquals("hello", p.parseUnquotedString());
    }

    @Test
    void terminatedByEof() {
      JsonParser p = parser("end of input");
      assertEquals("end of input", p.parseUnquotedString());
    }

    @Test
    void terminatedByCarriageReturn() {
      JsonParser p = parser("value\rrest");
      assertEquals("value", p.parseUnquotedString());
    }

    @Test
    void acrossBufferBoundary() {
      JsonParser p = parser("a long unquoted string value\n", 8);
      assertEquals("a long unquoted string value", p.parseUnquotedString());
    }

    @Test
    void emptyBeforeNewline() {
      JsonParser p = parser("\nrest");
      assertEquals("", p.parseUnquotedString());
    }
  }

  @Nested
  class ParseLiteralTests {
    @Test
    void trueFollowedByComma() {
      JsonParser p = parser("true,");
      JsonNode node = p.parseLiteral("true", BooleanNode.TRUE);
      assertSame(BooleanNode.TRUE, node);
    }

    @Test
    void falseFollowedByBrace() {
      JsonParser p = parser("false}");
      JsonNode node = p.parseLiteral("false", BooleanNode.FALSE);
      assertSame(BooleanNode.FALSE, node);
    }

    @Test
    void nullFollowedByBracket() {
      JsonParser p = parser("null]");
      JsonNode node = p.parseLiteral("null", NullNode.INSTANCE);
      assertSame(NullNode.INSTANCE, node);
    }

    @Test
    void trueFollowedByWhitespace() {
      JsonParser p = parser("true ");
      JsonNode node = p.parseLiteral("true", BooleanNode.TRUE);
      assertSame(BooleanNode.TRUE, node);
    }

    @Test
    void trueFollowedByNewline() {
      JsonParser p = parser("true\n");
      JsonNode node = p.parseLiteral("true", BooleanNode.TRUE);
      assertSame(BooleanNode.TRUE, node);
    }

    @Test
    void trueAtEof() {
      JsonParser p = parser("true");
      JsonNode node = p.parseLiteral("true", BooleanNode.TRUE);
      assertSame(BooleanNode.TRUE, node);
    }

    @Test
    void prefixMismatchFallsBackToUnquotedString() {
      JsonParser p = parser("tralala\n");
      JsonNode node = p.parseLiteral("true", BooleanNode.TRUE);
      assertTrue(node instanceof StringNode);
      assertEquals("tralala", node.asText());
    }

    @Test
    void invalidTerminationFallsBackToUnquotedString() {
      JsonParser p = parser("trueX\n");
      JsonNode node = p.parseLiteral("true", BooleanNode.TRUE);
      assertTrue(node instanceof StringNode);
      assertEquals("trueX", node.asText());
    }

    @Test
    void literalAcrossBufferBoundary() {
      JsonParser p = parser("  false,", 7);
      p.skipWhitespace();
      JsonNode node = p.parseLiteral("false", BooleanNode.FALSE);
      assertSame(BooleanNode.FALSE, node);
    }

    @Test
    void nullFollowedByColon() {
      JsonParser p = parser("null:");
      JsonNode node = p.parseLiteral("null", NullNode.INSTANCE);
      assertSame(NullNode.INSTANCE, node);
    }

    @Test
    void trueSpacesThenComma() {
      JsonParser p = parser("true   ,");
      JsonNode node = p.parseLiteral("true", BooleanNode.TRUE);
      assertSame(BooleanNode.TRUE, node);
    }

    @Test
    void falseTabsThenBracket() {
      JsonParser p = parser("false\t\t]");
      JsonNode node = p.parseLiteral("false", BooleanNode.FALSE);
      assertSame(BooleanNode.FALSE, node);
    }

    @Test
    void nullSpacesThenNewline() {
      JsonParser p = parser("null   \n");
      JsonNode node = p.parseLiteral("null", NullNode.INSTANCE);
      assertSame(NullNode.INSTANCE, node);
    }

    @Test
    void falseSpacesThenLetterFallsBack() {
      JsonParser p = parser("false  abc\n");
      JsonNode node = p.parseLiteral("false", BooleanNode.FALSE);
      assertInstanceOf(StringNode.class, node);
      assertEquals("falseabc", node.asText());
    }
  }

  @Nested
  class ParseNumberTests {
    @Test
    void zero() {
      JsonParser p = parser("0,");
      JsonNode node = p.parseNumber();
      assertInstanceOf(IntNode.class, node);
      assertEquals(0, node.asInt());
    }

    @Test
    void positiveInt() {
      JsonParser p = parser("42,");
      JsonNode node = p.parseNumber();
      assertInstanceOf(IntNode.class, node);
      assertEquals(42, node.asInt());
    }

    @Test
    void negativeInt() {
      JsonParser p = parser("-7,");
      JsonNode node = p.parseNumber();
      assertInstanceOf(IntNode.class, node);
      assertEquals(-7, node.asInt());
    }

    @Test
    void longValue() {
      JsonParser p = parser("9999999999,");
      JsonNode node = p.parseNumber();
      assertInstanceOf(LongNode.class, node);
      assertEquals(9999999999L, node.asLong());
    }

    @Test
    void doubleValue() {
      JsonParser p = parser("3.14,");
      JsonNode node = p.parseNumber();
      assertInstanceOf(DoubleNode.class, node);
      assertEquals(3.14, node.asDouble(), 0.0001);
    }

    @Test
    void exponentNotation() {
      JsonParser p = parser("1e10,");
      JsonNode node = p.parseNumber();
      assertInstanceOf(DoubleNode.class, node);
      assertEquals(1e10, node.asDouble(), 1.0);
    }

    @Test
    void negativeExponent() {
      JsonParser p = parser("5.5e-3,");
      JsonNode node = p.parseNumber();
      assertInstanceOf(DoubleNode.class, node);
      assertEquals(0.0055, node.asDouble(), 0.00001);
    }

    @Test
    void positiveExponent() {
      JsonParser p = parser("1.2E+4,");
      JsonNode node = p.parseNumber();
      assertInstanceOf(DoubleNode.class, node);
      assertEquals(12000.0, node.asDouble(), 0.1);
    }

    @Test
    void bigInteger() {
      JsonParser p = parser("99999999999999999999,"); // 20 digits
      JsonNode node = p.parseNumber();
      assertInstanceOf(BigIntegerNode.class, node);
      assertEquals("99999999999999999999", node.asText());
    }

    @Test
    void bigDecimalHugeFloat() {
      // > 25 chars → BigDecimalNode
      String huge = "1.23456789012345678901234567890";
      JsonParser p = parser(huge + ",");
      JsonNode node = p.parseNumber();
      assertInstanceOf(BigDecimalNode.class, node);
    }

    @Test
    void leadingZeroRejectsDoubleZero() {
      JsonParser p = parser("00\n");
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals("00", node.asText());
    }

    @Test
    void leadingZeroRejectsZeroOne() {
      JsonParser p = parser("01\n");
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals("01", node.asText());
    }

    @Test
    void leadingZeroAllowsZeroDot() {
      JsonParser p = parser("0.5,");
      JsonNode node = p.parseNumber();
      assertInstanceOf(DoubleNode.class, node);
      assertEquals(0.5, node.asDouble(), 0.0001);
    }

    @Test
    void invalidTerminationFallsBack() {
      JsonParser p = parser("123abc\n");
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals("123abc", node.asText());
    }

    @Test
    void minusOnlyFallsBack() {
      JsonParser p = parser("-\n");
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals("-", node.asText());
    }

    @Test
    void numberTerminatedByCloseBrace() {
      JsonParser p = parser("42}");
      JsonNode node = p.parseNumber();
      assertInstanceOf(IntNode.class, node);
      assertEquals(42, node.asInt());
    }

    @Test
    void numberTerminatedByCloseBracket() {
      JsonParser p = parser("42]");
      JsonNode node = p.parseNumber();
      assertInstanceOf(IntNode.class, node);
      assertEquals(42, node.asInt());
    }

    @Test
    void numberTerminatedByNewline() {
      JsonParser p = parser("42\n");
      JsonNode node = p.parseNumber();
      assertInstanceOf(IntNode.class, node);
      assertEquals(42, node.asInt());
    }

    @Test
    void numberTerminatedByColon() {
      JsonParser p = parser("42:");
      JsonNode node = p.parseNumber();
      assertInstanceOf(IntNode.class, node);
      assertEquals(42, node.asInt());
    }

    @Test
    void intInCacheRange() {
      JsonParser p = parser("5,");
      JsonNode node = p.parseNumber();
      assertSame(IntNode.valueOf(5), node);
    }

    @Test
    void trailingSpacesBeforeTerminator() {
      JsonParser p = parser("42  ,");
      JsonNode node = p.parseNumber();
      assertInstanceOf(IntNode.class, node);
      assertEquals(42, node.asInt());
      assertEquals(',', p.buf[p.ptr]);
    }

    @Test
    void numberAtEofGoesToSlowPath() {
      JsonParser p = parser("123");
      JsonNode node = p.parseNumber();
      assertInstanceOf(IntNode.class, node);
      assertEquals(123, node.asInt());
    }

    @Test
    void zeroPointZero() {
      JsonParser p = parser("0.0,");
      JsonNode node = p.parseNumber();
      assertInstanceOf(DoubleNode.class, node);
      assertEquals(0.0, node.asDouble(), 0.0001);
    }
  }

  @Nested
  class BufferBoundaryTests {
    @Test
    void quotedStringSpansBoundary() {
      String s = "\"abcdefghijklmnop\"";
      JsonParser p = parser(s, 8);
      assertEquals("abcdefghijklmnop", p.parseQuotedString());
    }

    @Test
    void longNumberSpansBoundary() {
      JsonParser p = parser("12345678901234,", 8);
      JsonNode node = p.parseNumber();
      assertEquals(12345678901234L, node.asLong());
    }

    @Test
    void unquotedStringSpansBoundary() {
      JsonParser p = parser("a long unquoted value\n", 8);
      assertEquals("a long unquoted value", p.parseUnquotedString());
    }

    @Test
    void literalFalseSpansBoundary() {
      JsonParser p = parser("  false,", 7);
      p.skipWhitespace();
      JsonNode node = p.parseLiteral("false", BooleanNode.FALSE);
      assertSame(BooleanNode.FALSE, node);
    }

    @Test
    void literalNullSpansBoundary() {
      JsonParser p = parser(" null}", 5);
      p.skipWhitespace();
      JsonNode node = p.parseLiteral("null", NullNode.INSTANCE);
      assertSame(NullNode.INSTANCE, node);
    }

    @Test
    void whitespaceWithNewlinesSpansBoundary() {
      String ws = "     \n     \n     x";
      JsonParser p = parser(ws, 4);
      p.skipWhitespace();
      assertEquals('x', p.buf[p.ptr]);
      assertEquals(3, p.lineNumber);
    }

    @Test
    void escapedStringSpansBoundary() {
      JsonParser p = parser("\"ab\\\"cd\"", 6);
      assertEquals("ab\"cd", p.parseQuotedString());
    }
  }

  @Nested
  class ErrorHelperTests {
    @Test
    void errorReportsLineAndColumn() {
      JsonParser p = parser("abc");
      p.lineNumber = 3;
      p.lineStart = 0;
      p.ptr = 5;
      var ex = p.error("test error");
      assertEquals(3, ex.getLine());
      assertEquals(6, ex.getColumn());
      assertTrue(ex.getMessage().contains("test error"));
    }
  }

  @Nested
  class ConvertNumberTests {
    @Test
    void intNodeForSmallInt() {
      JsonParser p = parser("");
      char[] src = "42".toCharArray();
      JsonNode node = p.convertNumber(src, 0, 2, 2, false);
      assertInstanceOf(IntNode.class, node);
      assertEquals(42, node.asInt());
    }

    @Test
    void longNodeForLargeInt() {
      JsonParser p = parser("");
      String s = "9999999999";
      char[] src = s.toCharArray();
      JsonNode node = p.convertNumber(src, 0, src.length, 10, false);
      assertInstanceOf(LongNode.class, node);
      assertEquals(9999999999L, node.asLong());
    }

    @Test
    void longThatFitsInIntDowncasts() {
      JsonParser p = parser("");
      String s = "1000000000"; // 10 digits, fits in int
      char[] src = s.toCharArray();
      JsonNode node = p.convertNumber(src, 0, src.length, 10, false);
      assertInstanceOf(IntNode.class, node);
      assertEquals(1000000000, node.asInt());
    }

    @Test
    void doubleNodeForFloat() {
      JsonParser p = parser("");
      String s = "3.14";
      char[] src = s.toCharArray();
      JsonNode node = p.convertNumber(src, 0, src.length, 0, true);
      assertInstanceOf(DoubleNode.class, node);
      assertEquals(3.14, node.asDouble(), 0.0001);
    }

    @Test
    void bigDecimalForHugeFloat() {
      JsonParser p = parser("");
      String s = "1.234567890123456789012345678";
      char[] src = s.toCharArray();
      JsonNode node = p.convertNumber(src, 0, src.length, 0, true);
      assertInstanceOf(BigDecimalNode.class, node);
    }

    @Test
    void bigIntegerForTwentyDigits() {
      JsonParser p = parser("");
      String s = "12345678901234567890";
      char[] src = s.toCharArray();
      JsonNode node = p.convertNumber(src, 0, src.length, 20, false);
      assertInstanceOf(BigIntegerNode.class, node);
    }
  }

  @Nested
  class NumberThresholdTests {

    @Test
    void atThresholdIntegerIsBigInteger() {
      String digits = "1".repeat(300);
      JsonParser p = parser(digits + ",");
      JsonNode node = p.parseNumber();
      assertInstanceOf(BigIntegerNode.class, node);
      assertEquals(digits, node.asText());
    }

    @Test
    void overThresholdIntegerIsString() {
      String digits = "1".repeat(301);
      JsonParser p = parser(digits + ",");
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals(digits, node.asText());
    }

    @Test
    void overThresholdFloatIsString() {
      String num = "1" + "." + "2".repeat(300);
      JsonParser p = parser(num + ",");
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals(num, node.asText());
    }

    @Test
    void overThresholdWithExponentIsString() {
      String num = "1".repeat(296) + "e+100";
      JsonParser p = parser(num + ",");
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals(num, node.asText());
    }

    @Test
    void overThresholdAtEofIsString() {
      String digits = "1".repeat(301);
      JsonParser p = parser(digits);
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals(digits, node.asText());
    }

    @Test
    void negativeOverThresholdIsString() {
      String num = "-" + "9".repeat(301);
      JsonParser p = parser(num + ",");
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals(num, node.asText());
    }

    @Test
    void overThresholdWithInvalidTerminationFastPath() {
      String digits = "1".repeat(301);
      JsonParser p = parser(digits + "abc\n");
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals(digits + "abc", node.asText());
    }

    @Test
    void exponentNoDigitsFastPath() {
      JsonParser p = parser("123e,");
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals("123e,", node.asText());
      assertEquals(5, p.ptr);
    }

    @Test
    void exponentNoDigitsSlowPath() {
      JsonParser p = parser("123e,", 4);
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals("123e,", node.asText());
    }

    @Test
    void exponentSignNoDigitsSlowPath() {
      JsonParser p = parser("4.5e+,", 4);
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals("4.5e+,", node.asText());
    }

    @Test
    void leadingZeroSlowPath() {
      JsonParser p = parser("007\n", 2);
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals("007", node.asText());
    }

    @Test
    void negativeLeadingZeroSlowPath() {
      JsonParser p = parser("-007\n", 3);
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals("-007", node.asText());
    }

    @Test
    void leadingZeroDotSlowPath() {
      JsonParser p = parser("0.5,", 2);
      JsonNode node = p.parseNumber();
      assertInstanceOf(DoubleNode.class, node);
      assertEquals(0.5, node.asDouble(), 0.0001);
    }

    @Test
    void negativeLeadingZeroMinusBoundary() {
      JsonParser p = parser("X-07\n", 2);
      p.ptr++;
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals("-07", node.asText());
    }

    @Test
    void exponentSignAcrossBoundary() {
      JsonParser p = parser("123e+5,", 4);
      JsonNode node = p.parseNumber();
      assertInstanceOf(DoubleNode.class, node);
      assertEquals(123e+5, node.asDouble(), 1.0);
    }

    @Test
    void exponentNegSignAcrossBoundary() {
      JsonParser p = parser("123e-2,", 4);
      JsonNode node = p.parseNumber();
      assertInstanceOf(DoubleNode.class, node);
      assertEquals(123e-2, node.asDouble(), 0.001);
    }

    @Test
    void minusOnlyAtEof() {
      JsonParser p = parser("-");
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals("-", node.asText());
    }

    @Test
    void exponentNoDigitsAtEof() {
      JsonParser p = parser("123e");
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals("123e", node.asText());
    }

    @Test
    void exponentSignNoDigitsAtEof() {
      JsonParser p = parser("123e+");
      JsonNode node = p.parseNumber();
      assertInstanceOf(StringNode.class, node);
      assertEquals("123e+", node.asText());
    }
  }
}
