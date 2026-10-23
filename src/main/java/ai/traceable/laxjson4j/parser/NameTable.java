package ai.traceable.laxjson4j.parser;

import java.util.concurrent.atomic.AtomicReference;

public final class NameTable {

  static final int HASH_MULT = 33;

  private static final int INITIAL_SIZE = 128;
  private static final int MAX_SIZE = 8192;
  private static final int MAX_PROBE = 16;

  private static final class TableState {
    final String[] names;
    final int[] hashes;
    final int mask;
    final int count;

    TableState(int size) {
      this.names = new String[size];
      this.hashes = new int[size];
      this.mask = size - 1;
      this.count = 0;
    }

    TableState(String[] names, int[] hashes, int mask, int count) {
      this.names = names;
      this.hashes = hashes;
      this.mask = mask;
      this.count = count;
    }
  }

  private final AtomicReference<TableState> rootState;

  private TableState state;
  private boolean hashShared;

  /** Root constructor — used by Json to create the shared NameTable. */
  public NameTable() {
    TableState initial = new TableState(INITIAL_SIZE);
    this.rootState = new AtomicReference<>(initial);
    this.state = initial;
    this.hashShared = false;
  }

  /** Child constructor — creates a thread-private child from a root snapshot. */
  private NameTable(AtomicReference<TableState> rootRef, TableState snapshot) {
    this.rootState = rootRef;
    this.state = snapshot;
    this.hashShared = true;
  }

  /** Creates a thread-private child that snapshots the current root state. */
  public NameTable makeChild() {
    return new NameTable(this.rootState, this.rootState.get());
  }

  /**
   * Refreshes this child's snapshot from the root. Call at parse-start. One atomic read, zero
   * allocations.
   */
  void resetFromRoot() {
    this.state = rootState.get();
    this.hashShared = true;
  }

  /**
   * Publishes new names back to the root. Call at parse-end. No-op if no new names were added
   * during this parse. Otherwise a single CAS attempt.
   */
  void mergeBack() {
    if (hashShared) return;
    TableState childState = this.state;
    while (true) {
      TableState root = rootState.get();
      if (childState.count <= root.count) return;
      if (rootState.compareAndSet(root, childState)) return;
    }
  }

  public static int calcHash(char[] buf, int start, int len) {
    int h = 0;
    for (int i = start, end = start + len; i < end; i++) {
      h = h * HASH_MULT + buf[i];
    }
    return h;
  }

  public String findName(char[] buf, int start, int len, int hash) {
    TableState s = this.state;
    int idx = hash & s.mask;

    for (int i = 0; i < MAX_PROBE; i++) {
      int slot = (idx + i) & s.mask;
      String name = s.names[slot];
      if (name == null) {
        return addName(buf, start, len, hash);
      }
      if (s.hashes[slot] == hash && matches(name, buf, start, len)) {
        return name;
      }
    }

    return addName(buf, start, len, hash);
  }

  private String addName(char[] buf, int start, int len, int hash) {
    if (hashShared) {
      TableState s = this.state;
      this.state = new TableState(s.names.clone(), s.hashes.clone(), s.mask, s.count);
      hashShared = false;
    }

    TableState s = this.state;
    int idx = hash & s.mask;
    for (int i = 0; i < MAX_PROBE; i++) {
      int slot = (idx + i) & s.mask;
      String name = s.names[slot];
      if (name == null) {
        if (s.count >= MAX_SIZE) {
          return new String(buf, start, len);
        }
        String newName = new String(buf, start, len);
        s.names[slot] = newName;
        s.hashes[slot] = hash;
        int newCount = s.count + 1;

        if (newCount > (s.mask + 1) * 3 / 4 && (s.mask + 1) < MAX_SIZE) {
          this.state = rehash(s.names, s.hashes, s.mask, newCount);
        } else {
          this.state = new TableState(s.names, s.hashes, s.mask, newCount);
        }
        return newName;
      }
      if (s.hashes[slot] == hash && matches(name, buf, start, len)) {
        return name;
      }
    }

    return new String(buf, start, len);
  }

  private static TableState rehash(String[] oldNames, int[] oldHashes, int oldMask, int count) {
    int newSize = (oldMask + 1) << 1;
    int newMask = newSize - 1;
    String[] newNames = new String[newSize];
    int[] newHashes = new int[newSize];

    for (int i = 0; i <= oldMask; i++) {
      String name = oldNames[i];
      if (name == null) continue;
      int h = oldHashes[i];
      int idx = h & newMask;
      while (newNames[idx] != null) {
        idx = (idx + 1) & newMask;
      }
      newNames[idx] = name;
      newHashes[idx] = h;
    }

    return new TableState(newNames, newHashes, newMask, count);
  }

  private static boolean matches(String name, char[] buf, int start, int len) {
    if (name.length() != len) return false;
    for (int i = 0; i < len; i++) {
      if (name.charAt(i) != buf[start + i]) return false;
    }
    return true;
  }
}
