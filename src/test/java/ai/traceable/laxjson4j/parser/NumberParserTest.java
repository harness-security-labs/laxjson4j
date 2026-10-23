package ai.traceable.laxjson4j.parser;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NumberParserTest {

  private static char[] chars(String s) {
    return s.toCharArray();
  }

  @Nested
  class ParseIntFastTests {
    @Test
    void zero() {
      assertEquals(0, NumberParser.parseInt(chars("0"), 0, 1));
    }

    @Test
    void singleDigit() {
      assertEquals(7, NumberParser.parseInt(chars("7"), 0, 1));
    }

    @Test
    void twoDigits() {
      assertEquals(42, NumberParser.parseInt(chars("42"), 0, 2));
    }

    @Test
    void threeDigits() {
      assertEquals(123, NumberParser.parseInt(chars("123"), 0, 3));
    }

    @Test
    void fiveDigits() {
      assertEquals(12345, NumberParser.parseInt(chars("12345"), 0, 5));
    }

    @Test
    void maxNineDigits() {
      assertEquals(999999999, NumberParser.parseInt(chars("999999999"), 0, 9));
    }

    @Test
    void nineDigitsMixed() {
      assertEquals(123456789, NumberParser.parseInt(chars("123456789"), 0, 9));
    }

    @Test
    void withOffset() {
      char[] buf = "xx123yy".toCharArray();
      assertEquals(123, NumberParser.parseInt(buf, 2, 3));
    }
  }

  @Nested
  class ParseLongSplitTests {
    @Test
    void tenDigits() {
      assertEquals(1234567890L, NumberParser.parseLong(chars("1234567890"), 0, 10));
    }

    @Test
    void elevenDigits() {
      assertEquals(12345678901L, NumberParser.parseLong(chars("12345678901"), 0, 11));
    }

    @Test
    void eighteenDigits() {
      assertEquals(123456789012345678L, NumberParser.parseLong(chars("123456789012345678"), 0, 18));
    }

    @Test
    void maxEighteenDigit() {
      assertEquals(999999999999999999L, NumberParser.parseLong(chars("999999999999999999"), 0, 18));
    }

    @Test
    void fitsInIntRange() {
      long val = NumberParser.parseLong(chars("2147483647"), 0, 10);
      assertEquals(Integer.MAX_VALUE, val);
    }

    @Test
    void justAboveIntRange() {
      long val = NumberParser.parseLong(chars("2147483648"), 0, 10);
      assertEquals(2147483648L, val);
    }
  }

  // ── inLongRange + parseLong19 ───────────────────────────────────────────

  @Nested
  class Long19Tests {
    @Test
    void nineteenDigitFitsPositive() {
      char[] ch = chars("1000000000000000000");
      assertTrue(NumberParser.inLongRange(ch, 0, 19, false));
      assertEquals(1000000000000000000L, NumberParser.parseLong19(ch, 0, false));
    }

    @Test
    void longMaxValue() {
      char[] ch = chars("9223372036854775807");
      assertTrue(NumberParser.inLongRange(ch, 0, 19, false));
      assertEquals(Long.MAX_VALUE, NumberParser.parseLong19(ch, 0, false));
    }

    @Test
    void longMaxValuePlusOneOverflows() {
      char[] ch = chars("9223372036854775808");
      assertFalse(NumberParser.inLongRange(ch, 0, 19, false));
    }

    @Test
    void allNinesOverflows() {
      char[] ch = chars("9999999999999999999");
      assertFalse(NumberParser.inLongRange(ch, 0, 19, false));
    }

    @Test
    void negativeLongMinValue() {
      char[] ch = chars("9223372036854775808");
      assertTrue(NumberParser.inLongRange(ch, 0, 19, true));
      assertEquals(Long.MIN_VALUE, NumberParser.parseLong19(ch, 0, true));
    }

    @Test
    void negativeLongMinValuePlusOneOverflows() {
      char[] ch = chars("9223372036854775809");
      assertFalse(NumberParser.inLongRange(ch, 0, 19, true));
    }

    @Test
    void negativeFits() {
      char[] ch = chars("1000000000000000000");
      assertTrue(NumberParser.inLongRange(ch, 0, 19, true));
      assertEquals(-1000000000000000000L, NumberParser.parseLong19(ch, 0, true));
    }

    @Test
    void exactBoundaryPositive() {
      char[] ch = chars("9223372036854775806");
      assertTrue(NumberParser.inLongRange(ch, 0, 19, false));
      assertEquals(9223372036854775806L, NumberParser.parseLong19(ch, 0, false));
    }
  }

  @Nested
  class ParseDoubleFastTests {
    @Test
    void simpleDecimal() {
      assertEquals(3.14, NumberParser.parseDouble(chars("3.14"), 0, 4), 1e-15);
    }

    @Test
    void negativeDecimal() {
      assertEquals(-0.5, NumberParser.parseDouble(chars("-0.5"), 0, 4), 1e-15);
    }

    @Test
    void withExponent() {
      assertEquals(1.5e10, NumberParser.parseDouble(chars("1.5e10"), 0, 6), 1e-5);
    }

    @Test
    void integerAsDouble() {
      assertEquals(42.0, NumberParser.parseDouble(chars("42.0"), 0, 4), 1e-15);
    }

    @Test
    void decimalNotFollowedByDigits() {
      assertEquals(42.0, NumberParser.parseDouble(chars("42."), 0, 3), 1e-15);
    }
  }

  @Nested
  class ParseBigDecimalTests {
    @Test
    void simpleValue() {
      BigDecimal bd = NumberParser.parseBigDecimal(chars("123.456"), 0, 7);
      assertEquals(new BigDecimal("123.456"), bd);
    }

    @Test
    void largeValue() {
      String s = "1" + "0".repeat(100) + ".5";
      char[] ch = chars(s);
      BigDecimal bd = NumberParser.parseBigDecimal(ch, 0, ch.length);
      assertEquals(new BigDecimal(s), bd);
    }
  }
}
