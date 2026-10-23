package ai.traceable.laxjson4j.convert;

import ai.traceable.laxjson4j.exception.JsonConversionException;
import ai.traceable.laxjson4j.node.ArrayNode;
import ai.traceable.laxjson4j.node.JsonNode;
import ai.traceable.laxjson4j.node.ObjectNode;
import ai.traceable.laxjson4j.node.StringNode;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class NodeConverter {

  private static final Map<Class<?>, Function<JsonNode, ?>> SCALAR_CONVERTERS = new HashMap<>();
  private static final Map<Class<?>, Object> PRIMITIVE_DEFAULTS = new HashMap<>();
  private static final ConcurrentHashMap<Class<?>, ClassInfo> CLASS_CACHE =
      new ConcurrentHashMap<>();

  static {
    SCALAR_CONVERTERS.put(String.class, JsonNode::asText);

    SCALAR_CONVERTERS.put(int.class, JsonNode::asInt);
    SCALAR_CONVERTERS.put(Integer.class, JsonNode::asInt);

    SCALAR_CONVERTERS.put(long.class, JsonNode::asLong);
    SCALAR_CONVERTERS.put(Long.class, JsonNode::asLong);

    SCALAR_CONVERTERS.put(double.class, JsonNode::asDouble);
    SCALAR_CONVERTERS.put(Double.class, JsonNode::asDouble);

    SCALAR_CONVERTERS.put(float.class, n -> (float) n.asDouble());
    SCALAR_CONVERTERS.put(Float.class, n -> (float) n.asDouble());

    SCALAR_CONVERTERS.put(boolean.class, JsonNode::asBoolean);
    SCALAR_CONVERTERS.put(Boolean.class, JsonNode::asBoolean);

    SCALAR_CONVERTERS.put(byte.class, n -> (byte) n.asInt());
    SCALAR_CONVERTERS.put(Byte.class, n -> (byte) n.asInt());

    SCALAR_CONVERTERS.put(short.class, n -> (short) n.asInt());
    SCALAR_CONVERTERS.put(Short.class, n -> (short) n.asInt());

    Function<JsonNode, Character> charConverter =
        n -> {
          String s = n.asText();
          return (s == null || s.isEmpty()) ? '\0' : s.charAt(0);
        };
    SCALAR_CONVERTERS.put(char.class, charConverter);
    SCALAR_CONVERTERS.put(Character.class, charConverter);

    SCALAR_CONVERTERS.put(
        BigInteger.class, n -> n.asBigInt() != null ? n.asBigInt() : new BigInteger(n.asText()));

    SCALAR_CONVERTERS.put(
        BigDecimal.class,
        n -> n.asBigDecimal() != null ? n.asBigDecimal() : new BigDecimal(n.asText()));

    SCALAR_CONVERTERS.put(Number.class, JsonNode::asNumber);

    SCALAR_CONVERTERS.put(Date.class, n -> convertDate(n, Date.class));
    SCALAR_CONVERTERS.put(Timestamp.class, n -> convertDate(n, Timestamp.class));
    SCALAR_CONVERTERS.put(Instant.class, n -> convertDate(n, Instant.class));
    SCALAR_CONVERTERS.put(LocalDate.class, n -> convertDate(n, LocalDate.class));
    SCALAR_CONVERTERS.put(LocalDateTime.class, n -> convertDate(n, LocalDateTime.class));

    PRIMITIVE_DEFAULTS.put(boolean.class, false);
    PRIMITIVE_DEFAULTS.put(char.class, '\0');
    PRIMITIVE_DEFAULTS.put(byte.class, (byte) 0);
    PRIMITIVE_DEFAULTS.put(short.class, (short) 0);
    PRIMITIVE_DEFAULTS.put(int.class, 0);
    PRIMITIVE_DEFAULTS.put(long.class, 0L);
    PRIMITIVE_DEFAULTS.put(float.class, 0.0f);
    PRIMITIVE_DEFAULTS.put(double.class, 0.0);
  }

  private NodeConverter() {}

  @SuppressWarnings("unchecked")
  public static <T> T convert(JsonNode node, Class<T> targetType) {
    return (T) convert(node, (Type) targetType);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static Object convert(JsonNode node, Type targetType) {
    if (node == null || node.isNull()) {
      return defaultForType(targetType);
    }

    Class<?> raw = TypeResolver.rawType(targetType);

    if (JsonNode.class.isAssignableFrom(raw)) {
      return node;
    }

    Function<JsonNode, ?> scalarFn = SCALAR_CONVERTERS.get(raw);
    if (scalarFn != null) {
      return scalarFn.apply(node);
    }

    if (raw.isEnum()) {
      return convertEnum(node, (Class<Enum>) raw);
    }

    if (Map.class.isAssignableFrom(raw)) {
      return convertToMap(node, targetType, raw);
    }

    if (Collection.class.isAssignableFrom(raw)) {
      return convertToCollection(node, targetType, raw);
    }

    if (raw.isArray()) {
      return convertToArray(node, raw.getComponentType());
    }

    return convertToBean(node, raw);
  }

  private static Object defaultForType(Type type) {
    Class<?> raw = TypeResolver.rawType(type);
    if (!raw.isPrimitive()) {
      return null;
    }
    return PRIMITIVE_DEFAULTS.get(raw);
  }

  static Object primitiveDefault(Class<?> type) {
    return PRIMITIVE_DEFAULTS.get(type);
  }

  static ClassInfo getClassInfo(Class<?> clazz) {
    return CLASS_CACHE.computeIfAbsent(clazz, ClassInfo::of);
  }

  private static Object convertToCollection(JsonNode node, Type targetType, Class<?> rawType) {
    if (!(node instanceof ArrayNode)) {
      throw new JsonConversionException(
          "Cannot convert " + node.type() + " to " + rawType.getName() + ": expected ARRAY node");
    }
    ArrayNode arr = (ArrayNode) node;
    Type elementType = TypeResolver.collectionElementType(targetType);
    Collection<Object> collection = createCollection(rawType, arr.size());
    Iterator<JsonNode> it = arr.elements();
    while (it.hasNext()) {
      collection.add(convert(it.next(), elementType));
    }
    return collection;
  }

  @SuppressWarnings("unchecked")
  private static Collection<Object> createCollection(Class<?> rawType, int sizeHint) {
    if (rawType == List.class || rawType == Collection.class || rawType == Iterable.class) {
      return new ArrayList<>(sizeHint);
    }
    if (rawType == Set.class) {
      return new LinkedHashSet<>(sizeHint);
    }
    try {
      return (Collection<Object>) rawType.getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      return new ArrayList<>(sizeHint);
    }
  }

  private static Object convertToMap(JsonNode node, Type targetType, Class<?> rawType) {
    if (!(node instanceof ObjectNode)) {
      throw new JsonConversionException(
          "Cannot convert " + node.type() + " to " + rawType.getName() + ": expected OBJECT node");
    }
    ObjectNode obj = (ObjectNode) node;
    Type keyType = TypeResolver.mapKeyType(targetType);
    Type valueType = TypeResolver.mapValueType(targetType);
    Class<?> keyRaw = TypeResolver.rawType(keyType);
    Map<Object, Object> map = createMap(rawType, obj.size());
    Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
    while (it.hasNext()) {
      Map.Entry<String, JsonNode> entry = it.next();
      Object key = convertMapKey(entry.getKey(), keyRaw);
      Object value = convert(entry.getValue(), valueType);
      map.put(key, value);
    }
    return map;
  }

  private static Object convertMapKey(String key, Class<?> keyType) {
    if (keyType == String.class || keyType == Object.class) {
      return key;
    }
    return convert(new StringNode(key), keyType);
  }

  @SuppressWarnings("unchecked")
  private static Map<Object, Object> createMap(Class<?> rawType, int sizeHint) {
    if (rawType == Map.class || rawType.isInterface()) {
      return new LinkedHashMap<>(sizeHint);
    }
    try {
      return (Map<Object, Object>) rawType.getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      return new LinkedHashMap<>(sizeHint);
    }
  }

  private static Object convertToArray(JsonNode node, Class<?> componentType) {
    if (!(node instanceof ArrayNode)) {
      throw new JsonConversionException(
          "Cannot convert "
              + node.type()
              + " to "
              + componentType.getName()
              + "[]: expected ARRAY node");
    }
    ArrayNode arr = (ArrayNode) node;
    int size = arr.size();
    Object array = Array.newInstance(componentType, size);
    for (int i = 0; i < size; i++) {
      Object value = convert(arr.get(i), componentType);
      Array.set(array, i, value);
    }
    return array;
  }

  private static Object convertToBean(JsonNode node, Class<?> targetType) {
    if (!(node instanceof ObjectNode)) {
      throw new JsonConversionException(
          "Cannot convert "
              + node.type()
              + " to "
              + targetType.getName()
              + ": expected OBJECT node");
    }
    ObjectNode obj = (ObjectNode) node;
    ClassInfo info = getClassInfo(targetType);
    Object instance = info.createInstance();
    for (ClassInfo.FieldInfo field : info.fields()) {
      JsonNode fieldNode = obj.get(field.jsonName());
      if (fieldNode != null && !fieldNode.isNull()) {
        Object value = convert(fieldNode, field.genericType());
        field.setValue(instance, value);
      }
    }
    return instance;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Object convertEnum(JsonNode node, Class<? extends Enum> enumType) {
    String text = node.asText();
    try {
      return Enum.valueOf(enumType, text);
    } catch (IllegalArgumentException e) {
      throw new JsonConversionException(
          "Invalid enum constant '" + text + "' for " + enumType.getName(), e);
    }
  }

  private static Object convertDate(JsonNode node, Class<?> targetType) {
    try {
      if (node.isNumber()) {
        long millis = node.asLong();
        return dateFromEpoch(millis, targetType);
      }
      String text = node.asText();
      return dateFromString(text, targetType);
    } catch (JsonConversionException e) {
      throw e;
    } catch (Exception e) {
      throw new JsonConversionException(
          "Cannot convert "
              + node.type()
              + " to "
              + targetType.getSimpleName()
              + ": unparseable value '"
              + node.asText()
              + "'",
          e);
    }
  }

  private static Object dateFromEpoch(long millis, Class<?> targetType) {
    if (targetType == Date.class) {
      return new Date(millis);
    }
    if (targetType == Timestamp.class) {
      return new Timestamp(millis);
    }
    Instant instant = Instant.ofEpochMilli(millis);
    if (targetType == Instant.class) {
      return instant;
    }
    if (targetType == LocalDate.class) {
      return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }
    if (targetType == LocalDateTime.class) {
      return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    throw new JsonConversionException("Unsupported date type: " + targetType.getName());
  }

  private static Object dateFromString(String text, Class<?> targetType) {
    if (targetType == Date.class) {
      return Date.from(Instant.parse(text));
    }
    if (targetType == Timestamp.class) {
      return Timestamp.from(Instant.parse(text));
    }
    if (targetType == Instant.class) {
      return Instant.parse(text);
    }
    if (targetType == LocalDate.class) {
      return LocalDate.parse(text);
    }
    if (targetType == LocalDateTime.class) {
      return LocalDateTime.parse(text);
    }
    throw new JsonConversionException("Unsupported date type: " + targetType.getName());
  }
}
