// Portions adapted and modified from Jackson (jackson-core, io/NumberInput.java).
// Copyright 2007-, Tatu Saloranta. Licensed under the Apache License, Version 2.0. See NOTICE.md.
package ai.traceable.laxjson4j.parser;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;
import java.math.BigDecimal;

public final class NumberParser {

  private static final long L_BILLION = 1_000_000_000L;

  private static final String MAX_LONG_STR = "9223372036854775807";
  private static final String MIN_LONG_STR_NO_SIGN = "9223372036854775808";

  private NumberParser() {}

  /** Fast parser for 1-9 unsigned digits (int range). */
  @SuppressWarnings("fallthrough")
  static int parseInt(char[] ch, int off, int len) {
    int num = ch[off + len - 1] - '0';
    switch (len) {
      case 9:
        num += (ch[off++] - '0') * 100_000_000;
      case 8:
        num += (ch[off++] - '0') * 10_000_000;
      case 7:
        num += (ch[off++] - '0') * 1_000_000;
      case 6:
        num += (ch[off++] - '0') * 100_000;
      case 5:
        num += (ch[off++] - '0') * 10_000;
      case 4:
        num += (ch[off++] - '0') * 1_000;
      case 3:
        num += (ch[off++] - '0') * 100;
      case 2:
        num += (ch[off] - '0') * 10;
    }
    return num;
  }

  /** Parser for 10-18 unsigned digits. Sign is handled by caller method. */
  static long parseLong(char[] ch, int off, int len) {
    int hi = len - 9;
    long val = (long) parseInt(ch, off, hi) * L_BILLION;
    return val + parseInt(ch, off + hi, 9);
  }

  /**
   * Lexicographic range check for exactly 19 unsigned digits. Returns true if the digit string is
   * within long range (considering sign).
   */
  static boolean inLongRange(char[] ch, int off, int len, boolean neg) {
    String cmp = neg ? MIN_LONG_STR_NO_SIGN : MAX_LONG_STR;
    int cmpLen = cmp.length();
    if (len < cmpLen) return true;
    if (len > cmpLen) return false;
    for (int i = 0; i < cmpLen; i++) {
      int diff = ch[off + i] - cmp.charAt(i);
      if (diff != 0) return (diff < 0);
    }
    return true;
  }

  // Specifically for 19-digit longs known to be in range.
  static long parseLong19(char[] ch, int off, boolean neg) {
    long num = 0L;
    for (int i = 0; i < 19; i++) {
      num = (num * 10) + (ch[off + i] - '0');
    }
    return neg ? -num : num;
  }

  static double parseDouble(char[] ch, int off, int len) {
    return JavaDoubleParser.parseDouble(ch, off, len);
  }

  static BigDecimal parseBigDecimal(char[] ch, int off, int len) {
    return new BigDecimal(ch, off, len);

    // // Possible optimisation
    // // JavaBigDecimalParser => highly efficient for longer decimal strings (>500 chars)
    // // In current version of parser, numeric parsing ensures that number string is at max ~300
    // // chars long, where JDK's conversion is faster
    // if (len < 500) {
    //   return new BigDecimal(ch, off, len);
    // }
    // return JavaBigDecimalParser.parseBigDecimal(ch, off, len);
  }
}
