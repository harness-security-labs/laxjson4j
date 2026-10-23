package ai.traceable.laxjson4j.parser;

public final class CharBuf {

  private static final int DEFAULT_CAPACITY = 4096;

  char[] data;
  int length;

  public CharBuf() {
    this(DEFAULT_CAPACITY);
  }

  public CharBuf(int initialCapacity) {
    this.data = new char[initialCapacity];
    this.length = 0;
  }

  public void append(char c) {
    if (length == data.length) {
      grow(length + 1);
    }
    data[length++] = c;
  }

  public void appendRange(char[] src, int off, int len) {
    if (len <= 0) return;
    int required = length + len;
    if (required > data.length) {
      grow(required);
    }
    System.arraycopy(src, off, data, length, len);
    length += len;
  }

  public void reset() {
    length = 0;
  }

  public int capacity() {
    return data.length;
  }

  @Override
  public String toString() {
    return new String(data, 0, length);
  }

  public String toStringStripped() {
    return toString().strip();
  }

  private void grow(int minCapacity) {
    int newCap = data.length << 1;
    if (newCap < minCapacity) {
      newCap = minCapacity;
    }
    char[] newData = new char[newCap];
    System.arraycopy(data, 0, newData, 0, length);
    data = newData;
  }
}
