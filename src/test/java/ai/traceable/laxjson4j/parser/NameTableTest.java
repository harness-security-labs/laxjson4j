package ai.traceable.laxjson4j.parser;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class NameTableTest {

  // ── Existing tests (root used directly, single-threaded) ────────────

  @Test
  void findName_returnsSameReferenceForRepeatedLookup() {
    NameTable table = new NameTable();
    char[] buf = "username".toCharArray();
    int hash = NameTable.calcHash(buf, 0, buf.length);

    String first = table.findName(buf, 0, buf.length, hash);
    String second = table.findName(buf, 0, buf.length, hash);

    assertEquals("username", first);
    assertSame(first, second, "Repeated lookup must return the identical cached String");
  }

  @Test
  void findName_cachesDifferentNamesIndependently() {
    NameTable table = new NameTable();

    char[] buf1 = "id".toCharArray();
    int hash1 = NameTable.calcHash(buf1, 0, buf1.length);
    String name1 = table.findName(buf1, 0, buf1.length, hash1);

    char[] buf2 = "email".toCharArray();
    int hash2 = NameTable.calcHash(buf2, 0, buf2.length);
    String name2 = table.findName(buf2, 0, buf2.length, hash2);

    assertEquals("id", name1);
    assertEquals("email", name2);
    assertNotSame(name1, name2);
    assertSame(name1, table.findName(buf1, 0, buf1.length, hash1));
    assertSame(name2, table.findName(buf2, 0, buf2.length, hash2));
  }

  @Test
  void findName_respectsOffsetAndLength() {
    NameTable table = new NameTable();
    char[] buf = "xxxnamexxx".toCharArray();
    int start = 3;
    int len = 4;
    int hash = NameTable.calcHash(buf, start, len);

    String result = table.findName(buf, start, len, hash);
    assertEquals("name", result);

    char[] buf2 = "name".toCharArray();
    int hash2 = NameTable.calcHash(buf2, 0, 4);
    assertSame(
        result,
        table.findName(buf2, 0, 4, hash2),
        "Same logical name from different buffer position must resolve to cached entry");
  }

  @Test
  void findName_cachesManyConcurrentEntries() {
    NameTable table = new NameTable();
    int count = 50;
    String[] keys = new String[count];
    String[] cached = new String[count];

    for (int i = 0; i < count; i++) {
      keys[i] = "field_" + i;
      char[] buf = keys[i].toCharArray();
      int hash = NameTable.calcHash(buf, 0, buf.length);
      cached[i] = table.findName(buf, 0, buf.length, hash);
    }

    for (int i = 0; i < count; i++) {
      char[] buf = keys[i].toCharArray();
      int hash = NameTable.calcHash(buf, 0, buf.length);
      assertSame(
          cached[i],
          table.findName(buf, 0, buf.length, hash),
          "Entry '" + keys[i] + "' must remain cached");
    }
  }

  @Test
  void calcHash_isDeterministicAndDistinguishesInputs() {
    char[] a = "status".toCharArray();
    char[] b = "status".toCharArray();
    char[] c = "Status".toCharArray();

    int hashA = NameTable.calcHash(a, 0, a.length);
    int hashB = NameTable.calcHash(b, 0, b.length);
    int hashC = NameTable.calcHash(c, 0, c.length);

    assertEquals(hashA, hashB, "Same content must produce same hash");
    assertNotEquals(hashA, hashC, "Different content should produce different hash");

    char[] padded = "xxstatusxx".toCharArray();
    assertEquals(hashA, NameTable.calcHash(padded, 2, 6));
  }

  // ── Parent-child model tests ────────────────────────────────────────

  @Test
  void child_inheritsRootNames() {
    NameTable root = new NameTable();
    String interned = intern(root, "userId");

    NameTable child = root.makeChild();
    String fromChild = intern(child, "userId");

    assertSame(interned, fromChild, "Child must see names added to root before makeChild");
  }

  @Test
  void child_isolation_twoChildrenDoNotSeeEachOtherNewNames() {
    NameTable root = new NameTable();
    intern(root, "shared");

    NameTable child1 = root.makeChild();
    NameTable child2 = root.makeChild();

    child1.resetFromRoot();
    child2.resetFromRoot();

    String onlyInChild1 = intern(child1, "onlyChild1");
    String onlyInChild2 = intern(child2, "onlyChild2");

    assertNotNull(onlyInChild1);
    assertNotNull(onlyInChild2);

    char[] buf1 = "onlyChild1".toCharArray();
    int h1 = NameTable.calcHash(buf1, 0, buf1.length);
    String lookupInChild2 = child2.findName(buf1, 0, buf1.length, h1);
    assertNotSame(
        onlyInChild1,
        lookupInChild2,
        "child2 must not see child1's private name (different String instance)");
  }

  @Test
  void mergeBack_publishesNewNamesToRoot() {
    NameTable root = new NameTable();
    NameTable child = root.makeChild();

    child.resetFromRoot();
    String added = intern(child, "newField");
    child.mergeBack();

    NameTable child2 = root.makeChild();
    child2.resetFromRoot();
    String fromChild2 = intern(child2, "newField");

    assertSame(added, fromChild2, "After mergeBack, new child must see previously merged names");
  }

  @Test
  void mergeBack_noOpWhenNoNewNames() {
    NameTable root = new NameTable();
    intern(root, "existing");

    NameTable child = root.makeChild();
    child.resetFromRoot();

    intern(child, "existing");

    child.mergeBack();

    NameTable child2 = root.makeChild();
    child2.resetFromRoot();
    String fromChild2 = intern(child2, "existing");
    assertEquals("existing", fromChild2);
  }

  @Test
  void resetFromRoot_refreshesWithLatestRootState() {
    NameTable root = new NameTable();

    NameTable child1 = root.makeChild();
    child1.resetFromRoot();
    String name1 = intern(child1, "alpha");
    child1.mergeBack();

    NameTable child2 = root.makeChild();
    child2.resetFromRoot();
    String name2 = intern(child2, "beta");
    child2.mergeBack();

    NameTable child3 = root.makeChild();
    child3.resetFromRoot();
    assertSame(name1, intern(child3, "alpha"), "child3 must see alpha from first merge");
    assertSame(name2, intern(child3, "beta"), "child3 must see beta from second merge");
  }

  @Test
  void resetFromRoot_canBeCalledMultipleTimesOnSameChild() {
    NameTable root = new NameTable();
    NameTable child = root.makeChild();

    child.resetFromRoot();
    String first = intern(child, "round1");
    child.mergeBack();

    child.resetFromRoot();
    String second = intern(child, "round2");
    child.mergeBack();

    NameTable verifier = root.makeChild();
    verifier.resetFromRoot();
    assertSame(first, intern(verifier, "round1"));
    assertSame(second, intern(verifier, "round2"));
  }

  @Test
  void concurrentChildren_allNamesEventuallyVisible() throws Exception {
    NameTable root = new NameTable();
    int threadCount = 8;
    int namesPerThread = 20;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    Set<String> allNames = Collections.newSetFromMap(new ConcurrentHashMap<>());

    for (int t = 0; t < threadCount; t++) {
      final int threadId = t;
      executor.submit(
          () -> {
            try {
              startLatch.await();
              NameTable child = root.makeChild();
              for (int round = 0; round < 3; round++) {
                child.resetFromRoot();
                for (int n = 0; n < namesPerThread; n++) {
                  String key = "t" + threadId + "_n" + n;
                  allNames.add(key);
                  intern(child, key);
                }
                child.mergeBack();
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              doneLatch.countDown();
            }
          });
    }

    startLatch.countDown();
    assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Threads must complete within timeout");
    executor.shutdown();

    NameTable verifier = root.makeChild();
    verifier.resetFromRoot();
    for (String key : allNames) {
      char[] buf = key.toCharArray();
      int hash = NameTable.calcHash(buf, 0, buf.length);
      String found = verifier.findName(buf, 0, buf.length, hash);
      assertEquals(key, found, "Name '" + key + "' must be resolvable after concurrent merges");
    }
  }

  // ── Helper ──────────────────────────────────────────────────────────

  private static String intern(NameTable table, String name) {
    char[] buf = name.toCharArray();
    int hash = NameTable.calcHash(buf, 0, buf.length);
    return table.findName(buf, 0, buf.length, hash);
  }
}
