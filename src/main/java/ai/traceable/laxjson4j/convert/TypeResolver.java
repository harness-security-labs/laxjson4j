package ai.traceable.laxjson4j.convert;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

public final class TypeResolver {

  private static final Type[] EMPTY_TYPE_ARRAY = new Type[0];

  private TypeResolver() {}

  public static Class<?> rawType(Type type) {
    if (type instanceof Class<?>) {
      return (Class<?>) type;
    }
    if (type instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) type;
      return (Class<?>) pt.getRawType();
    }
    if (type instanceof GenericArrayType) {
      GenericArrayType gat = (GenericArrayType) type;
      Class<?> component = rawType(gat.getGenericComponentType());
      return Array.newInstance(component, 0).getClass();
    }
    if (type instanceof WildcardType) {
      WildcardType wt = (WildcardType) type;
      Type[] upper = wt.getUpperBounds();
      return (upper.length > 0) ? rawType(upper[0]) : Object.class;
    }
    if (type instanceof TypeVariable<?>) {
      TypeVariable<?> tv = (TypeVariable<?>) type;
      Type[] bounds = tv.getBounds();
      return (bounds.length > 0) ? rawType(bounds[0]) : Object.class;
    }
    return Object.class;
  }

  public static Type[] typeArguments(Type type) {
    if (type instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) type;
      return pt.getActualTypeArguments();
    }
    return EMPTY_TYPE_ARRAY;
  }

  public static Type collectionElementType(Type type) {
    Type[] args = typeArguments(type);
    return args.length > 0 ? args[0] : Object.class;
  }

  public static Type mapKeyType(Type type) {
    Type[] args = typeArguments(type);
    return args.length > 0 ? args[0] : String.class;
  }

  public static Type mapValueType(Type type) {
    Type[] args = typeArguments(type);
    return args.length > 1 ? args[1] : Object.class;
  }
}
