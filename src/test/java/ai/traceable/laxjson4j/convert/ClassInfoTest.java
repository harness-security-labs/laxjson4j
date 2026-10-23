package ai.traceable.laxjson4j.convert;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ClassInfoTest {

  @SuppressWarnings("unused")
  static class FlatFields {
    String name;
    int count;
    private double score;
    static String STATIC_FIELD = "ignored";
    transient int transientField;
  }

  static class Parent {
    String parentField;
  }

  static class Child extends Parent {
    String childField;
  }

  static class NoDefaultCtor {
    String name;
    int value;

    NoDefaultCtor(String name, int value) {
      this.name = name;
      this.value = value;
    }
  }

  @Test
  void fieldDiscoverySkipsStaticAndTransient() {
    ClassInfo info = ClassInfo.of(FlatFields.class);
    ClassInfo.FieldInfo[] fields = info.fields();
    assertEquals(3, fields.length);

    Set<String> names =
        Arrays.stream(fields).map(ClassInfo.FieldInfo::jsonName).collect(Collectors.toSet());
    assertTrue(names.contains("name"));
    assertTrue(names.contains("count"));
    assertTrue(names.contains("score"));
    assertFalse(names.contains("STATIC_FIELD"));
    assertFalse(names.contains("transientField"));
  }

  @Test
  void inheritanceWalksToParent() {
    ClassInfo info = ClassInfo.of(Child.class);
    Set<String> names =
        Arrays.stream(info.fields()).map(ClassInfo.FieldInfo::jsonName).collect(Collectors.toSet());
    assertTrue(names.contains("childField"));
    assertTrue(names.contains("parentField"));
    assertEquals(2, info.fields().length);
  }

  @Test
  void defaultConstructorPreferred() {
    ClassInfo info = ClassInfo.of(FlatFields.class);
    Object instance = info.createInstance();
    assertInstanceOf(FlatFields.class, instance);

    FlatFields flat = (FlatFields) instance;
    assertNull(flat.name);
    assertEquals(0, flat.count);
  }

  @Test
  void fallbackToSmallestConstructor() {
    ClassInfo info = ClassInfo.of(NoDefaultCtor.class);
    Object instance = info.createInstance();
    assertInstanceOf(NoDefaultCtor.class, instance);

    NoDefaultCtor obj = (NoDefaultCtor) instance;
    assertNull(obj.name);
    assertEquals(0, obj.value);
  }
}
