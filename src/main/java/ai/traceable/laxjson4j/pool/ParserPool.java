package ai.traceable.laxjson4j.pool;

import ai.traceable.laxjson4j.parser.CharBuf;
import ai.traceable.laxjson4j.parser.JsonParser;
import ai.traceable.laxjson4j.parser.NameTable;

public final class ParserPool {

  private static final int BUFFER_SIZE = 32768;
  private static final int TEXT_BUF_INITIAL = 4096;
  private static final int MAX_RETAIN_CAPACITY = 65536;

  private final char[] inputBuffer;
  private CharBuf textBuffer;
  private JsonParser parser;

  public ParserPool(NameTable rootNameTable) {
    this.inputBuffer = new char[BUFFER_SIZE];
    this.textBuffer = new CharBuf(TEXT_BUF_INITIAL);
    NameTable child = rootNameTable.makeChild();
    this.parser = new JsonParser(inputBuffer, textBuffer, child);
  }

  public JsonParser parser() {
    return parser;
  }

  public void trimIfOversized() {
    if (textBuffer.capacity() > MAX_RETAIN_CAPACITY) {
      textBuffer = new CharBuf(TEXT_BUF_INITIAL);
      parser = new JsonParser(inputBuffer, textBuffer, parser.nameTable());
    }
  }
}
