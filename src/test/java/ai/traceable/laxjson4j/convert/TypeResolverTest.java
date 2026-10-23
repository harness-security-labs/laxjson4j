package ai.traceable.laxjson4j.convert;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TypeResolverTest {

  @SuppressWarnings("unused")
  List<String> stringList;

  @SuppressWarnings("unused")
  List<Integer> integerList;

  @SuppressWarnings("unused")
  Map<String, Integer> stringIntMap;

  @SuppressWarnings("unused")
  Map<String, List<Integer>> nestedMap;

  @SuppressWarnings("unused")
  List<?> wildcardList;

  private Type fieldType(String name) {
    try {
      return getClass().getDeclaredField(name).getGenericType();
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void rawTypeResolution() {
    assertEquals(String.class, TypeResolver.rawType(String.class));
    assertEquals(List.class, TypeResolver.rawType(fieldType("stringList")));
    assertEquals(Object.class, TypeResolver.rawType(null));

    // WildcardType: List<?> has type argument ? which upper-bounds to Object
    Type wildcardArg = TypeResolver.typeArguments(fieldType("wildcardList"))[0];
    assertEquals(Object.class, TypeResolver.rawType(wildcardArg));

    // Raw class passes through
    assertEquals(int.class, TypeResolver.rawType(int.class));
  }

  @Test
  void typeArgumentsFromParameterizedType() {
    Type[] args = TypeResolver.typeArguments(fieldType("stringList"));
    assertEquals(1, args.length);
    assertEquals(String.class, args[0]);

    // Non-parameterized type returns empty
    assertEquals(0, TypeResolver.typeArguments(String.class).length);
  }

  @Test
  void collectionElementTypeResolution() {
    assertEquals(Integer.class, TypeResolver.collectionElementType(fieldType("integerList")));
    assertEquals(Object.class, TypeResolver.collectionElementType(List.class));
  }

  @Test
  void mapKeyAndValueTypeResolution() {
    Type mapType = fieldType("stringIntMap");
    assertEquals(String.class, TypeResolver.mapKeyType(mapType));
    assertEquals(Integer.class, TypeResolver.mapValueType(mapType));

    // Raw Map defaults
    assertEquals(String.class, TypeResolver.mapKeyType(Map.class));
    assertEquals(Object.class, TypeResolver.mapValueType(Map.class));
  }

  @Test
  void nestedParameterizedTypes() {
    Type mapType = fieldType("nestedMap");
    Type valueType = TypeResolver.mapValueType(mapType);

    assertInstanceOf(ParameterizedType.class, valueType);
    assertEquals(List.class, TypeResolver.rawType(valueType));

    Type elementType = TypeResolver.collectionElementType(valueType);
    assertEquals(Integer.class, elementType);
  }
}
