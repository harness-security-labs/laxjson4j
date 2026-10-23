package ai.traceable.laxjson4j.convert;

import ai.traceable.laxjson4j.exception.JsonConversionException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class ClassInfo {

  private final Class<?> rawType;
  private final Constructor<?> constructor;
  private final FieldInfo[] fields;

  public static final class FieldInfo {
    private final String jsonName;
    private final Field field;
    private final Type genericType;
    private final Class<?> rawType;

    FieldInfo(String jsonName, Field field, Type genericType, Class<?> rawType) {
      this.jsonName = jsonName;
      this.field = field;
      this.genericType = genericType;
      this.rawType = rawType;
    }

    public String jsonName() {
      return jsonName;
    }

    public Field field() {
      return field;
    }

    public Type genericType() {
      return genericType;
    }

    public Class<?> rawType() {
      return rawType;
    }

    void setValue(Object instance, Object value) {
      try {
        field.set(instance, value);
      } catch (IllegalAccessException e) {
        throw new JsonConversionException(
            "Cannot set field '" + jsonName + "' on " + instance.getClass().getName(), e);
      }
    }
  }

  private ClassInfo(Class<?> rawType, Constructor<?> constructor, FieldInfo[] fields) {
    this.rawType = rawType;
    this.constructor = constructor;
    this.fields = fields;
  }

  public static ClassInfo of(Class<?> clazz) {
    FieldInfo[] fields = discoverFields(clazz);
    Constructor<?> ctor = selectConstructor(clazz);
    return new ClassInfo(clazz, ctor, fields);
  }

  public Object createInstance() {
    try {
      if (constructor.getParameterCount() == 0) {
        return constructor.newInstance();
      }
      Class<?>[] paramTypes = constructor.getParameterTypes();
      Object[] params = new Object[paramTypes.length];
      for (int i = 0; i < paramTypes.length; i++) {
        if (paramTypes[i].isPrimitive()) {
          params[i] = NodeConverter.primitiveDefault(paramTypes[i]);
        }
      }
      return constructor.newInstance(params);
    } catch (ReflectiveOperationException e) {
      throw new JsonConversionException("Cannot instantiate " + rawType.getName(), e);
    }
  }

  public FieldInfo[] fields() {
    return fields;
  }

  public Constructor<?> constructor() {
    return constructor;
  }

  private static FieldInfo[] discoverFields(Class<?> clazz) {
    List<FieldInfo> result = new ArrayList<>();
    Class<?> current = clazz;
    while (current != null
        && current != Object.class
        && !current.getName().startsWith("java.lang.")) {
      for (Field f : current.getDeclaredFields()) {
        int mods = f.getModifiers();
        if (Modifier.isStatic(mods) || Modifier.isTransient(mods)) {
          continue;
        }
        f.setAccessible(true);
        result.add(new FieldInfo(f.getName(), f, f.getGenericType(), f.getType()));
      }
      current = current.getSuperclass();
    }
    return result.toArray(new FieldInfo[0]);
  }

  private static Constructor<?> selectConstructor(Class<?> clazz) {
    try {
      Constructor<?> ctor = clazz.getDeclaredConstructor();
      ctor.setAccessible(true);
      return ctor;
    } catch (NoSuchMethodException e) {
      Constructor<?>[] ctors = clazz.getDeclaredConstructors();
      if (ctors.length == 0) {
        throw new JsonConversionException("No available constructor for " + clazz.getName());
      }
      Arrays.sort(ctors, Comparator.comparingInt(Constructor::getParameterCount));
      Constructor<?> ctor = ctors[0];
      ctor.setAccessible(true);
      return ctor;
    }
  }
}
