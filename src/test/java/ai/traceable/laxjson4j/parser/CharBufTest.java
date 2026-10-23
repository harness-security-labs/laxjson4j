package ai.traceable.laxjson4j.parser;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CharBufTest {

  @Nested
  class AppendTests {
    @Test
    void appendSingleChar() {
      CharBuf buf = new CharBuf(4);
      buf.append('a');
      assertEquals("a", buf.toString());
    }

    @Test
    void appendMultipleChars() {
      CharBuf buf = new CharBuf(4);
      buf.append('h');
      buf.append('i');
      assertEquals("hi", buf.toString());
    }

    @Test
    void appendRangeBulkCopy() {
      CharBuf buf = new CharBuf(16);
      char[] src = "hello world".toCharArray();
      buf.appendRange(src, 0, src.length);
      assertEquals("hello world", buf.toString());
    }

    @Test
    void appendRangeWithOffset() {
      CharBuf buf = new CharBuf(16);
      char[] src = "hello world".toCharArray();
      buf.appendRange(src, 6, 5);
      assertEquals("world", buf.toString());
    }

    @Test
    void appendRangeZeroLength() {
      CharBuf buf = new CharBuf(4);
      buf.append('x');
      buf.appendRange("abc".toCharArray(), 0, 0);
      assertEquals("x", buf.toString());
    }
  }

  @Nested
  class GrowthTests {
    @Test
    void growsBeyondInitialCapacity() {
      CharBuf buf = new CharBuf(4);
      for (int i = 0; i < 100; i++) {
        buf.append((char) ('a' + (i % 26)));
      }
      assertEquals(100, buf.toString().length());
    }

    @Test
    void appendRangeTriggersGrowth() {
      CharBuf buf = new CharBuf(4);
      char[] large = new char[50];
      for (int i = 0; i < large.length; i++) large[i] = 'z';
      buf.appendRange(large, 0, large.length);
      assertEquals(50, buf.toString().length());
    }
  }

  @Nested
  class ResetTests {
    @Test
    void resetClearsLength() {
      CharBuf buf = new CharBuf(8);
      buf.append('a');
      buf.append('b');
      buf.reset();
      assertEquals("", buf.toString());
      assertEquals(0, buf.length);
    }

    @Test
    void reuseAfterReset() {
      CharBuf buf = new CharBuf(8);
      buf.append('a');
      buf.append('b');
      buf.reset();
      buf.append('x');
      assertEquals("x", buf.toString());
    }
  }

  @Nested
  class ToStringTests {
    @Test
    void emptyBuf() {
      CharBuf buf = new CharBuf(4);
      assertEquals("", buf.toString());
    }

    @Test
    void toStringStrippedTrimsWhitespace() {
      CharBuf buf = new CharBuf(16);
      char[] src = "  hello  ".toCharArray();
      buf.appendRange(src, 0, src.length);
      assertEquals("hello", buf.toStringStripped());
    }

    @Test
    void toStringStrippedNoTrim() {
      CharBuf buf = new CharBuf(8);
      char[] src = "abc".toCharArray();
      buf.appendRange(src, 0, src.length);
      assertEquals("abc", buf.toStringStripped());
    }
  }
}
