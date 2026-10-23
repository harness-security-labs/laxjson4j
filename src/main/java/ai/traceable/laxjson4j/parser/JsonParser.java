package ai.traceable.laxjson4j.parser;

import ai.traceable.laxjson4j.exception.JsonParseException;
import ai.traceable.laxjson4j.node.*;
import java.math.BigInteger;

public final class JsonParser {

  // Constants

  private static final char OBJECT_OPEN = '{';
  private static final char OBJECT_CLOSE = '}';
  private static final char ARRAY_OPEN = '[';
  private static final char ARRAY_CLOSE = ']';
  private static final char COMMA = ',';
  private static final char COLON = ':';
  private static final char QUOTE = '"';
  private static final char BACKSLASH = '\\';
  private static final char SLASH = '/';
  private static final char ASTERISK = '*';

  // Fields

  private static final int MAX_NUMBER_LENGTH = 300;

  // Lookup table for fast paths in string parsing
  // Should be accessed via a final reference field for JIT compilation to kick in (as array bounds
  // become known at compile time)
  private static final int[] CHAR_CLASS = new int[128];

  static {
    CHAR_CLASS['"'] = 1;
    CHAR_CLASS['\\'] = 2;
    CHAR_CLASS['\n'] = 3;
    CHAR_CLASS['\r'] = 3;
  }

  char[] buf;
  int ptr;
  int end;

  private String input;
  private int inputOffset;
  private int inputLength;

  int lineNumber;
  int lineStart;

  private final CharBuf textBuf;
  private final NameTable nameTable;

  // ── Constructor ────────────────────────────────────────────────────────

  public JsonParser(char[] buf, CharBuf textBuf, NameTable nameTable) {
    this.buf = buf;
    this.textBuf = textBuf;
    this.nameTable = nameTable;
  }

  public NameTable nameTable() {
    return nameTable;
  }

  public void init(String json) {
    this.input = json;
    this.inputLength = json.length();
    int firstChunk = Math.min(buf.length, inputLength);
    json.getChars(0, firstChunk, buf, 0);
    this.ptr = 0;
    this.end = firstChunk;
    this.inputOffset = firstChunk;
    this.lineNumber = 1;
    this.lineStart = 0;
    this.textBuf.reset();
  }

  // ── Utility methods ────────────────────────────────────────────────────

  // Buffer management methods

  private boolean loadMore() {
    if (inputOffset >= inputLength) {
      return false;
    }
    lineStart -= end;
    int toRead = Math.min(buf.length, inputLength - inputOffset);
    input.getChars(inputOffset, inputOffset + toRead, buf, 0);
    inputOffset += toRead;
    ptr = 0;
    end = toRead;
    return true;
  }

  private boolean compactAndLoad() {
    if (inputOffset >= inputLength) {
      return false;
    }
    int remaining = end - ptr;
    if (remaining > 0) {
      System.arraycopy(buf, ptr, buf, 0, remaining);
    }
    lineStart -= ptr;
    int space = buf.length - remaining;
    int toRead = Math.min(space, inputLength - inputOffset);
    input.getChars(inputOffset, inputOffset + toRead, buf, remaining);
    inputOffset += toRead;
    ptr = 0;
    end = remaining + toRead;
    return true;
  }

  // Whitespace and comment skipping

  void skipWhitespace() {
    int p = ptr;
    // Cache buf as local — saves one field load per character in the inner loop.
    // Same technique as Gson's nextNonWhitespace; see that method's comment for rationale.
    final char[] b = buf;

    outer:
    while (true) {
      while (p < end) {
        char c = b[p];
        // Fast exit: any char above ASCII 0x20 other than '/' is guaranteed
        // non-whitespace. Excluding '/' preserves lenient // and /* */ comment
        // handling below while still short-circuiting the common case.
        if (c > ' ' && c != SLASH) break outer;
        if (c == ' ' || c == '\t') {
          p++;
          continue;
        }
        if (c == '\n') {
          lineNumber++;
          p++;
          lineStart = p;
          continue;
        }
        if (c == '\r') {
          lineNumber++;
          p++;
          if (p < end) {
            if (b[p] == '\n') p++;
          } else {
            ptr = p;
            if (loadMore()) {
              p = ptr;
              if (p < end && b[p] == '\n') p++;
            }
          }
          lineStart = p;
          continue;
        }
        if (c == SLASH) {
          if (p + 1 < end) {
            char next = b[p + 1];
            if (next == SLASH) {
              p = skipLineComment(p + 2);
              continue;
            }
            if (next == ASTERISK) {
              p = skipBlockComment(p + 2);
              continue;
            }
          } else {
            // SLASH at buffer edge — need to peek at next char for comment start
            ptr = p;
            if (compactAndLoad()) {
              p = ptr;
              continue;
            }
            // No more input — SLASH is content, not a comment
          }
        }
        break outer;
      }

      ptr = p;
      if (!loadMore()) break;
      p = ptr;
    }
    ptr = p;
  }

  private int skipLineComment(int p) {
    while (true) {
      while (p < end) {
        char c = buf[p];
        if (c == '\n' || c == '\r') {
          return p;
        }
        p++;
      }
      ptr = p;
      if (!loadMore()) return ptr;
      p = ptr;
    }
  }

  private int skipBlockComment(int p) {
    while (true) {
      while (p < end) {
        char c = buf[p];
        if (c == ASTERISK) {
          if (p + 1 < end) {
            if (buf[p + 1] == SLASH) {
              return p + 2;
            }
            p++;
          } else {
            // ASTERISK at buffer edge — preserve it and reload to peek at next char
            ptr = p;
            if (compactAndLoad()) {
              p = ptr;
              continue;
            }
            return ptr;
          }
        } else if (c == '\n') {
          lineNumber++;
          p++;
          lineStart = p;
        } else if (c == '\r') {
          lineNumber++;
          p++;
          if (p < end) {
            if (buf[p] == '\n') p++;
          } else {
            ptr = p;
            if (loadMore()) {
              p = ptr;
              if (p < end && buf[p] == '\n') p++;
            }
          }
          lineStart = p;
        } else {
          p++;
        }
      }
      ptr = p;
      if (!loadMore()) return ptr;
      p = ptr;
    }
  }

  private void skipInlineSpaces() {
    while (true) {
      while (ptr < end && (buf[ptr] == ' ' || buf[ptr] == '\t')) ptr++;
      if (ptr < end || !loadMore()) break;
    }
  }

  // Structural helpers

  private static boolean isStructuralTerminator(char c) {
    return c == COMMA
        || c == OBJECT_CLOSE
        || c == ARRAY_CLOSE
        || c == COLON
        || c == '\n'
        || c == '\r';
  }

  private void drainNumberChars() {
    while (true) {
      while (ptr < end) {
        char c = buf[ptr];
        if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
          ptr++;
        } else {
          while (ptr < end && (buf[ptr] == ' ' || buf[ptr] == '\t')) ptr++;
          return;
        }
      }
      if (!loadMore()) return;
    }
  }

  // Error helper

  JsonParseException error(String msg) {
    return new JsonParseException(msg, lineNumber, ptr - lineStart + 1);
  }

  // ── parse (public entry points) ──────────────────────────────────────

  public JsonNode parse(String json) {
    init(json);
    nameTable.resetFromRoot();
    try {
      return parse();
    } finally {
      nameTable.mergeBack();
    }
  }

  public JsonNode parse() {
    skipWhitespace();
    JsonNode root = parseValue();
    if (!(root instanceof ObjectNode) && !(root instanceof ArrayNode)) {
      throw error("JSON root must be an object or array");
    }
    return root;
  }

  // parseValue(): the main parsing loop

  private JsonNode parseValue() {
    if (ptr >= end) return NullNode.INSTANCE;

    switch (buf[ptr]) {
      case OBJECT_OPEN:
        return parseObject();
      case ARRAY_OPEN:
        return parseArray();
      case QUOTE:
        return new StringNode(parseQuotedString());
      case 't':
        return parseLiteral("true", BooleanNode.TRUE);
      case 'f':
        return parseLiteral("false", BooleanNode.FALSE);
      case 'n':
        return parseLiteral("null", NullNode.INSTANCE);
      case '-':
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
        return parseNumber();
      default:
        return new StringNode(parseUnquotedString());
    }
  }

  // parse Json Objects

  private ObjectNode parseObject() {
    ptr++;
    ObjectNode obj = new ObjectNode();
    // buf never changes after init; caching it eliminates one field load per buf[ptr] access.
    final char[] b = buf;

    while (true) {
      skipWhitespace();
      if (ptr >= end) break;
      if (b[ptr] == OBJECT_CLOSE) {
        ptr++;
        return obj;
      }

      char c = b[ptr];
      if (c == COMMA) {
        ptr++;
        continue;
      }
      if (c == COLON || c == ARRAY_OPEN || c == OBJECT_OPEN || c == ARRAY_CLOSE) {
        throw error("Object key must be a string, found: '" + c + "'");
      }

      String key;
      if (b[ptr] == QUOTE) {
        key = parseFieldName();
      } else {
        key = parseUnquotedFieldName();
      }

      skipWhitespace();
      if (ptr >= end || b[ptr] != COLON) {
        char found = ptr < end ? b[ptr] : ' ';
        throw error("Expected ':' after object key '" + key + "', found: '" + found + "'");
      }
      ptr++;

      skipWhitespace();
      if (ptr >= end) break;
      if (b[ptr] == OBJECT_CLOSE) {
        ptr++;
        return obj;
      }
      JsonNode value = parseValue();
      obj.put(key, value);

      skipWhitespace();
      if (ptr < end && b[ptr] == COMMA) ptr++;
    }

    return obj;
  }

  // parse Json Arrays

  private ArrayNode parseArray() {
    ptr++;
    ArrayNode arr = new ArrayNode();
    final char[] b = buf;

    while (true) {
      skipWhitespace();
      if (ptr >= end) break;
      if (b[ptr] == ARRAY_CLOSE) {
        ptr++;
        return arr;
      }

      if (b[ptr] == COMMA || b[ptr] == COLON || b[ptr] == OBJECT_CLOSE) {
        ptr++;
        continue;
      }

      JsonNode value = parseValue();
      arr.add(value);

      skipWhitespace();
      if (ptr < end && b[ptr] == COMMA) ptr++;
    }

    return arr;
  }

  // parse Quoted Strings (fast path)

  String parseQuotedString() {
    // Cache fields as locals — reduces field reads in the tight inner loop.
    // Technique from Gson's JsonReader (see nextQuotedValue / peekNumber comments).
    int p = ++ptr;
    int start = p;
    final int e = end;
    final char[] b = buf;
    final int[] cc = CHAR_CLASS;

    while (p < e) {
      char c = b[p];
      if (c < 128) {
        int cls = cc[c];
        if (cls == 0) {
          p++;
          continue;
        }
        if (cls == 1) {
          // Close quote — fast path: plain ASCII string, no escapes.
          String result = new String(b, start, p - start);
          ptr = p + 1;
          return result;
        }
        if (cls == 2) {
          // Backslash — fall to slow path, sync ptr first.
          ptr = p;
          return parseQuotedStringSlow(start);
        }
        // Control character or other special — return up to here.
        ptr = p;
        return new String(b, start, p - start);
      }
      p++;
    }

    ptr = p;
    return parseQuotedStringSlow(start);
  }

  private String parseQuotedStringSlow(int start) {
    textBuf.reset();
    final char[] b = buf;
    int prefixLen = ptr - start;
    if (prefixLen > 0) {
      textBuf.appendRange(b, start, prefixLen);
    }

    final int[] charClass = CHAR_CLASS;
    while (true) {
      // Bulk-scan: find the next special character instead of processing one char at a time.
      // Plain ASCII runs are copied wholesale via appendRange / System.arraycopy.
      int runStart = ptr;
      while (ptr < end) {
        char c = b[ptr];
        if (c < 128) {
          int cls = charClass[c];
          if (cls == 0) {
            ptr++;
            continue;
          }
          // Flush the plain-text run accumulated so far
          if (ptr > runStart) textBuf.appendRange(b, runStart, ptr - runStart);
          if (cls == 1) {
            ptr++;
            return textBuf.toString();
          }
          if (cls == 3) {
            return textBuf.toString();
          }
          // cls == 2: backslash
          ptr++;
          if (ptr >= end && !loadMore()) {
            textBuf.append(BACKSLASH);
            return textBuf.toString();
          }
          handleEscapeSequence();
          runStart = ptr;
          continue;
        }
        // Non-ASCII: flush run, append char, continue
        if (ptr > runStart) textBuf.appendRange(b, runStart, ptr - runStart);
        textBuf.append(c);
        ptr++;
        runStart = ptr;
      }
      // Flush whatever plain-text run reached the buffer edge
      if (ptr > runStart) textBuf.appendRange(b, runStart, ptr - runStart);

      if (!loadMore()) break;
    }

    return textBuf.toString();
  }

  private void handleEscapeSequence() {
    char c = buf[ptr];
    switch (c) {
      case QUOTE:
      case BACKSLASH:
      case SLASH:
        textBuf.append(c);
        ptr++;
        break;
      case 'b':
        textBuf.append('\b');
        ptr++;
        break;
      case 't':
        textBuf.append('\t');
        ptr++;
        break;
      case 'n':
        textBuf.append('\n');
        ptr++;
        break;
      case 'f':
        textBuf.append('\f');
        ptr++;
        break;
      case 'r':
        textBuf.append('\r');
        ptr++;
        break;
      case 'u':
        ptr++; // skip 'u'
        if (ptr + 4 > end) {
          compactAndLoad();
        }
        if (ptr + 4 <= end) {
          int codePoint = 0;
          for (int i = 0; i < 4; i++) {
            codePoint = (codePoint << 4) | hexDigit(buf[ptr + i]);
          }
          textBuf.append((char) codePoint);
          ptr += 4;
        } else {
          // Not enough hex digits available — emit literal backslash-u and whatever is there
          textBuf.append(BACKSLASH);
          textBuf.append('u');
        }
        break;
      case '\n':
      case '\r':
        // Backslash before newline — emit backslash, don't consume newline
        textBuf.append(BACKSLASH);
        break;
      default:
        textBuf.append(BACKSLASH);
        textBuf.append(c);
        ptr++;
        break;
    }
  }

  private int hexDigit(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'f') return c - 'a' + 10;
    if (c >= 'A' && c <= 'F') return c - 'A' + 10;
    throw error("Invalid hex digit in unicode escape: " + c);
  }

  // parse Unquoted Strings (fast path)

  String parseUnquotedString() {
    int start = ptr;
    while (ptr < end) {
      char c = buf[ptr];
      if (c == '\n' || c == '\r') break;
      ptr++;
    }

    if (ptr < end) {
      return new String(buf, start, ptr - start).strip();
    }

    textBuf.reset();
    return parseUnquotedStringSlow(start);
  }

  private String parseUnquotedStringSlow(int start) {
    int prefixLen = ptr - start;
    if (prefixLen > 0) {
      textBuf.appendRange(buf, start, prefixLen);
    }

    while (true) {
      while (ptr < end) {
        char c = buf[ptr];
        if (c == '\n' || c == '\r') {
          return textBuf.toStringStripped();
        }
        textBuf.append(buf[ptr++]);
      }
      if (!loadMore()) break;
    }

    return textBuf.toStringStripped();
  }

  // parse Field Names (quoted key with symbol table)

  private String parseFieldName() {
    int p = ++ptr;
    int start = p;
    int hash = 0;
    final int e = end;
    final char[] b = buf;
    final int[] cc = CHAR_CLASS;

    while (p < e) {
      char c = b[p];
      if (c < 128) {
        int cls = cc[c];
        if (cls == 0) {
          hash = hash * NameTable.HASH_MULT + c;
          p++;
          continue;
        }
        if (cls == 1) {
          int len = p - start;
          ptr = p + 1;
          return nameTable.findName(b, start, len, hash);
        }
        if (cls == 2) {
          ptr = p;
          return parseFieldNameSlow(start);
        }
        ptr = p;
        return nameTable.findName(b, start, p - start, hash);
      }
      hash = hash * NameTable.HASH_MULT + c;
      p++;
    }

    ptr = p;
    return parseFieldNameSlow(start);
  }

  private String parseFieldNameSlow(int start) {
    textBuf.reset();
    final char[] b = buf;
    int prefixLen = ptr - start;
    if (prefixLen > 0) {
      textBuf.appendRange(b, start, prefixLen);
    }

    final int[] charClass = CHAR_CLASS;
    while (true) {
      int runStart = ptr;
      while (ptr < end) {
        char c = b[ptr];
        if (c < 128) {
          int cls = charClass[c];
          if (cls == 0) {
            ptr++;
            continue;
          }
          if (ptr > runStart) textBuf.appendRange(b, runStart, ptr - runStart);
          if (cls == 1) {
            ptr++;
            return internFromTextBuf();
          }
          if (cls == 3) {
            return internFromTextBuf();
          }
          // cls == 2: backslash
          ptr++;
          if (ptr >= end && !loadMore()) {
            textBuf.append(BACKSLASH);
            return internFromTextBuf();
          }
          handleEscapeSequence();
          runStart = ptr;
          continue;
        }
        if (ptr > runStart) textBuf.appendRange(b, runStart, ptr - runStart);
        textBuf.append(c);
        ptr++;
        runStart = ptr;
      }
      if (ptr > runStart) textBuf.appendRange(b, runStart, ptr - runStart);

      if (!loadMore()) break;
    }

    return internFromTextBuf();
  }

  private String internFromTextBuf() {
    return internName(textBuf.data, 0, textBuf.length);
  }

  private String internName(char[] data, int start, int end) {
    int s = start, e = end;
    while (s < e && data[s] <= ' ') s++;
    while (e > s && data[e - 1] <= ' ') e--;
    int len = e - s;
    int h = NameTable.calcHash(data, s, len);
    return nameTable.findName(data, s, len, h);
  }

  // parse Unquoted Field Names (unquoted key with symbol table)

  private String parseUnquotedFieldName() {
    int start = ptr;
    while (ptr < end) {
      char c = buf[ptr];
      if (c == '\n' || c == '\r') break;
      ptr++;
    }

    if (ptr < end) {
      return internName(buf, start, ptr);
    }

    textBuf.reset();
    return parseUnquotedFieldNameSlow(start);
  }

  private String parseUnquotedFieldNameSlow(int start) {
    int prefixLen = ptr - start;
    if (prefixLen > 0) {
      textBuf.appendRange(buf, start, prefixLen);
    }

    while (true) {
      while (ptr < end) {
        char c = buf[ptr];
        if (c == '\n' || c == '\r') {
          return internFromTextBuf();
        }
        textBuf.append(buf[ptr++]);
      }
      if (!loadMore()) break;
    }

    return internFromTextBuf();
  }

  // parse Literals (true, false, null). fallsback to parseUnquotedString() if the literal is not
  // found

  JsonNode parseLiteral(String literal, JsonNode node) {
    int len = literal.length();

    if (ptr + len > end) {
      compactAndLoad();
    }

    if (ptr + len > end) {
      return new StringNode(parseUnquotedString());
    }

    for (int i = 0; i < len; i++) {
      if (buf[ptr + i] != literal.charAt(i)) {
        return new StringNode(parseUnquotedString());
      }
    }

    // Literal chars matched — advance past them
    ptr += len;

    // Skip trailing spaces/tabs (may cross buffer boundary)
    skipInlineSpaces();

    // Check structural terminator or EOF
    if (ptr >= end) {
      return node; // EOF after literal is valid
    }
    if (isStructuralTerminator(buf[ptr])) {
      return node;
    }

    // Not a valid literal — build unquoted string from here, including the literal itself
    // any spaces between the literal and the structural terminator are skipped here
    // example: "true    abc" -> "trueabc"
    textBuf.reset();
    for (int i = 0; i < len; i++) {
      textBuf.append(literal.charAt(i));
    }
    return new StringNode(parseUnquotedStringSlow(ptr));
  }

  // parse Numbers (integers and floats)

  JsonNode parseNumber() {
    int start = ptr;
    boolean isFloat = false;

    // Step 1: optional minus
    if (buf[ptr] == '-') {
      ptr++;
      if (ptr >= end) return parseNumberSlow(start);
    }

    // Step 2: leading zero validation
    if (buf[ptr] == '0') {
      if (ptr + 1 >= end) {
        ptr++;
        return parseNumberSlow(start);
      }
      char next = buf[ptr + 1];
      if (next != '.' && next != ' ' && next != '\t' && !isStructuralTerminator(next)) {
        ptr = start;
        return new StringNode(parseUnquotedString());
      }
    }

    // Step 3: integer digits
    int intStart = ptr;
    while (ptr < end && buf[ptr] >= '0' && buf[ptr] <= '9') ptr++;
    if (ptr >= end) return parseNumberSlow(start);
    int intLen = ptr - intStart;
    if (intLen == 0 && ptr > start) {
      // minus with no digits (e.g. just "-")
      ptr = start;
      return new StringNode(parseUnquotedString());
    }

    // Step 4: optional fraction
    if (buf[ptr] == '.') {
      isFloat = true;
      ptr++;
      if (ptr >= end) return parseNumberSlow(start);
      while (ptr < end && buf[ptr] >= '0' && buf[ptr] <= '9') ptr++;
      if (ptr >= end) return parseNumberSlow(start);
    }

    // Step 5: optional exponent
    if (buf[ptr] == 'e' || buf[ptr] == 'E') {
      isFloat = true;
      ptr++;
      if (ptr >= end) return parseNumberSlow(start);
      if (buf[ptr] == '+' || buf[ptr] == '-') {
        ptr++;
        if (ptr >= end) return parseNumberSlow(start);
      }
      int expStart = ptr;
      while (ptr < end && buf[ptr] >= '0' && buf[ptr] <= '9') ptr++;
      if (ptr >= end) return parseNumberSlow(start);
      if (ptr == expStart) {
        ptr = start;
        return new StringNode(parseUnquotedString());
      }
    }

    // Step 6: skip trailing spaces/tabs
    int trailingStart = ptr;
    while (ptr < end && (buf[ptr] == ' ' || buf[ptr] == '\t')) ptr++;
    if (ptr >= end) return parseNumberSlow(start);

    // Step 7: validate termination
    char term = buf[ptr];
    if (!isStructuralTerminator(term)) {
      ptr = start;
      return new StringNode(parseUnquotedString());
    }

    // Step 8: threshold check
    if (trailingStart - start > MAX_NUMBER_LENGTH) {
      return new StringNode(new String(buf, start, trailingStart - start));
    }

    // Step 9: convert
    return convertNumber(buf, start, trailingStart, intLen, isFloat);
  }

  private JsonNode parseNumberSlow(int start) {
    textBuf.reset();

    int prefixLen = ptr - start;
    if (prefixLen > 0) {
      textBuf.appendRange(buf, start, prefixLen);
    }

    if (!loadMore()) {
      if (textBuf.length == 0) return new StringNode("");
      if (textBuf.length > MAX_NUMBER_LENGTH) return new StringNode(textBuf.toString());
      char lastCh = textBuf.data[textBuf.length - 1];
      if (lastCh < '0' || lastCh > '9') return new StringNode(textBuf.toString());
      boolean eofFloat = false;
      int eofIntLen = 0;
      boolean eofPastDot = false, eofPastExp = false;
      for (int i = 0; i < textBuf.length; i++) {
        char c = textBuf.data[i];
        if (c == '.') {
          eofFloat = true;
          eofPastDot = true;
        } else if (c == 'e' || c == 'E') {
          eofFloat = true;
          eofPastExp = true;
        } else if (c >= '0' && c <= '9' && !eofPastDot && !eofPastExp) eofIntLen++;
      }
      return convertNumber(textBuf.data, 0, textBuf.length, eofIntLen, eofFloat);
    }

    // Leading zero validation (mirrors fast path step 2)
    if ((textBuf.length == 1 && textBuf.data[0] == '0')
        || (textBuf.length == 2 && textBuf.data[0] == '-' && textBuf.data[1] == '0')) {
      if (ptr < end) {
        char next = buf[ptr];
        if (next != '.' && next != ' ' && next != '\t' && !isStructuralTerminator(next)) {
          return new StringNode(parseUnquotedStringSlow(ptr));
        }
      }
    }

    // Leading zero when '-' was last char in previous buffer
    if (textBuf.length == 1 && textBuf.data[0] == '-') {
      if (ptr < end && buf[ptr] == '0') {
        if (ptr + 1 < end) {
          char next = buf[ptr + 1];
          if (next != '.' && next != ' ' && next != '\t' && !isStructuralTerminator(next)) {
            textBuf.append(buf[ptr++]);
            return new StringNode(parseUnquotedStringSlow(ptr));
          }
        }
      }
    }

    // Determine grammar state and integer digit count from prefix
    boolean seenDot = false;
    boolean seenExp = false;
    int intLen = 0;

    for (int i = 0; i < textBuf.length; i++) {
      char c = textBuf.data[i];
      if (c == '.') seenDot = true;
      else if (c == 'e' || c == 'E') seenExp = true;
      else if (c >= '0' && c <= '9' && !seenDot && !seenExp) intLen++;
    }

    // Continue scanning from fresh buffer based on grammar state
    if (seenExp) {
      if (ptr < end && (buf[ptr] == '+' || buf[ptr] == '-')) {
        textBuf.append(buf[ptr++]);
      }
      while (ptr < end && buf[ptr] >= '0' && buf[ptr] <= '9') {
        textBuf.append(buf[ptr++]);
      }
    } else if (seenDot) {
      while (ptr < end && buf[ptr] >= '0' && buf[ptr] <= '9') {
        textBuf.append(buf[ptr++]);
      }
      if (ptr < end && (buf[ptr] == 'e' || buf[ptr] == 'E')) {
        seenExp = true;
        textBuf.append(buf[ptr++]);
        if (ptr < end && (buf[ptr] == '+' || buf[ptr] == '-')) {
          textBuf.append(buf[ptr++]);
        }
        while (ptr < end && buf[ptr] >= '0' && buf[ptr] <= '9') {
          textBuf.append(buf[ptr++]);
        }
      }
    } else {
      while (ptr < end && buf[ptr] >= '0' && buf[ptr] <= '9') {
        textBuf.append(buf[ptr++]);
        intLen++;
      }
      if (ptr < end && buf[ptr] == '.') {
        seenDot = true;
        textBuf.append(buf[ptr++]);
        while (ptr < end && buf[ptr] >= '0' && buf[ptr] <= '9') {
          textBuf.append(buf[ptr++]);
        }
      }
      if (ptr < end && (buf[ptr] == 'e' || buf[ptr] == 'E')) {
        seenExp = true;
        textBuf.append(buf[ptr++]);
        if (ptr < end && (buf[ptr] == '+' || buf[ptr] == '-')) {
          textBuf.append(buf[ptr++]);
        }
        while (ptr < end && buf[ptr] >= '0' && buf[ptr] <= '9') {
          textBuf.append(buf[ptr++]);
        }
      }
    }

    // Exponent digit validation — only when scanner stopped at a non-continuation char,
    // not when buffer was exhausted (ptr >= end falls through to drain + threshold guard)
    if (ptr < end && textBuf.length > 0) {
      char last = textBuf.data[textBuf.length - 1];
      if (last == 'e' || last == 'E' || last == '+' || last == '-') {
        return new StringNode(parseUnquotedStringSlow(ptr));
      }
    }

    // Buffer > MAX_NUMBER_LENGTH is guaranteed, so if buffer is exhausted
    // the number has crossed the threshold — drain remaining number chars
    if (ptr >= end) {
      drainNumberChars();
    }

    // Skip trailing spaces/tabs
    while (ptr < end && (buf[ptr] == ' ' || buf[ptr] == '\t')) ptr++;

    // Validate termination
    if (ptr < end) {
      char term = buf[ptr];
      if (!isStructuralTerminator(term)) {
        while (ptr < end) {
          char c = buf[ptr];
          if (c == '\n' || c == '\r') break;
          textBuf.append(buf[ptr++]);
        }
        while (ptr >= end && loadMore()) {
          while (ptr < end) {
            char c = buf[ptr];
            if (c == '\n' || c == '\r') break;
            textBuf.append(buf[ptr++]);
          }
          if (ptr < end) break;
        }
        return new StringNode(textBuf.toStringStripped());
      }
    }

    // Final guards — ensure convertNumber only receives valid structures
    if (textBuf.length == 0) return new StringNode("");
    if (textBuf.length > MAX_NUMBER_LENGTH) return new StringNode(textBuf.toString());
    char lastCh = textBuf.data[textBuf.length - 1];
    if (lastCh < '0' || lastCh > '9') return new StringNode(textBuf.toString());
    return convertNumber(textBuf.data, 0, textBuf.length, intLen, seenDot || seenExp);
  }

  // convertNumber(): convert the number to the appropriate node type

  JsonNode convertNumber(char[] source, int start, int numEnd, int intLen, boolean isFloat) {
    int numLen = numEnd - start;
    boolean neg = (source[start] == '-');
    int digitStart = neg ? start + 1 : start;

    if (isFloat) {
      if (numLen > 25) {
        return new BigDecimalNode(new String(source, start, numLen));
      }
      double val = NumberParser.parseDouble(source, start, numLen);
      return new DoubleNode(val);
    }

    // Integer path
    if (intLen <= 9) {
      int val = NumberParser.parseInt(source, digitStart, intLen);
      return IntNode.valueOf(neg ? -val : val);
    }

    if (intLen <= 18) {
      long val = NumberParser.parseLong(source, digitStart, intLen);
      if (neg) val = -val;
      if (intLen == 10 && val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
        return IntNode.valueOf((int) val);
      }
      return new LongNode(val);
    }

    if (intLen == 19) {
      if (NumberParser.inLongRange(source, digitStart, intLen, neg)) {
        long val = NumberParser.parseLong19(source, digitStart, neg);
        return new LongNode(val);
      }
    }

    // 20+ digits or 19-digit overflow
    String raw = new String(source, start, numLen);
    return new BigIntegerNode(new BigInteger(raw));
  }
}
