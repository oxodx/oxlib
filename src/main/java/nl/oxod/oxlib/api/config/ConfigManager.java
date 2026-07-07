package nl.oxod.oxlib.api.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import nl.oxod.oxlib.OxLib;
import nl.oxod.oxlib.api.config.decorators.IgnoreField;

public final class ConfigManager {
  public static <T> T load(T config, ConfigurationSection section) {
    Field[] fields = config.getClass().getDeclaredFields();
    for (Field field : fields) {
      if (field.isAnnotationPresent(IgnoreField.class)) {
        continue;
      }

      field.setAccessible(true);
      String key = serializeKey(field.getName());

      try {
        if (Map.class.isAssignableFrom(field.getType())) {
          handleMapLoading(field, config, section.getConfigurationSection(key));
        } else if (List.class.isAssignableFrom(field.getType())) {
          handleListLoading(field, config, section, key);
        } else if (Set.class.isAssignableFrom(field.getType())) {
          handleSetLoading(field, config, section, key);
        } else if (isPrimitiveOrWrapper(field.getType())) {
          Object value = loadPrimitiveType(config, field, section, key);
          field.set(config, value);
        } else if (ConfigurationSection.class.isAssignableFrom(field.getType())) {
          field.set(config, section.getConfigurationSection(key));
        } else {
          ConfigurationSection nestedSection = section.getConfigurationSection(key);
          if (nestedSection != null) {
            Object nestedObject = field.getType().newInstance();
            load(nestedObject, nestedSection);
            field.set(config, nestedObject);
          }
        }
      } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
        e.printStackTrace();
      }
    }

    return config;
  }

  private static void handleMapLoading(Field field, Object config, ConfigurationSection section) {
    Type genericType = field.getGenericType();
    if (!(genericType instanceof ParameterizedType)) {
      return;
    }

    ParameterizedType parameterizedType = (ParameterizedType) genericType;
    Type[] typeArguments = parameterizedType.getActualTypeArguments();
    if (typeArguments.length != 2) {
      return;
    }

    Class<?> keyType = (Class<?>) typeArguments[0];
    Class<?> valueType = (Class<?>) typeArguments[1];

    Map<Object, Object> map = new HashMap<>();

    if (section == null) {
      try {
        field.setAccessible(true);
        if (field.get(config) == null) {
          field.set(config, map);
        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
      return;
    }

    for (String keyString : section.getKeys(false)) {
      Object mapKey = convertKey(keyType, keyString);
      if (mapKey == null) {
        continue;
      }

      Object mapValue;
      if (isPrimitiveOrWrapper(valueType)) {
        mapValue = getPrimitive(valueType, section, keyString);
      } else if (ConfigurationSection.class.isAssignableFrom(valueType)) {
        mapValue = section.getConfigurationSection(keyString);
      } else {
        ConfigurationSection subSection = section.getConfigurationSection(keyString);
        if (subSection != null) {
          try {
            mapValue = valueType.getDeclaredConstructor().newInstance();
            load(mapValue, subSection);
          } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            continue;
          }
        } else {
          continue;
        }
      }
      map.put(mapKey, mapValue);
    }

    try {
      field.setAccessible(true);
      field.set(config, map);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  private static ConfigurationSection deepMapToConfigurationSection(Map<?, ?> map) {
    ConfigurationSection section = new MemoryConfiguration();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (entry.getValue() instanceof Map) {
        section.set(entry.getKey().toString(), deepMapToConfigurationSection((Map<?, ?>) entry.getValue()));
      } else {
        section.set(entry.getKey().toString(), entry.getValue());
      }
    }
    return section;
  }

  private static void handleListLoading(Field field, Object config, ConfigurationSection section, String key)
      throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    List<Object> items = loadCollectionItems(field, section, key);
    if (items != null) {
      field.set(config, items);
    } else if (field.get(config) == null) {
      field.set(config, new ArrayList<>());
    }
  }

  private static void handleSetLoading(Field field, Object config, ConfigurationSection section, String key)
      throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    List<Object> items = loadCollectionItems(field, section, key);
    if (items != null) {
      field.set(config, new HashSet<>(items));
    } else if (field.get(config) == null) {
      field.set(config, new HashSet<>());
    }
  }

  private static List<Object> loadCollectionItems(Field field, ConfigurationSection section, String key)
      throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    List<?> raw = section.getList(key);
    if (raw == null) {
      return null;
    }

    Type genericType = field.getGenericType();
    if (!(genericType instanceof ParameterizedType parameterizedType)) {
      return null;
    }
    if (!(parameterizedType.getActualTypeArguments()[0] instanceof Class<?> itemType)) {
      return null;
    }

    List<Object> items = new ArrayList<>();
    for (Object elem : raw) {
      if (elem instanceof Map map) {
        Object item = itemType.getDeclaredConstructor().newInstance();
        load(item, deepMapToConfigurationSection(map));
        items.add(item);
      } else if (isPrimitiveOrWrapper(itemType)) {
        items.add(loadPrimitiveType(itemType, elem));
      }
    }
    return items;
  }

  public static String serializeKey(String fieldName) {
    return fieldName.replaceAll("([a-z])([A-Z]+)", "$1-$2").toLowerCase(Locale.ROOT);
  }

  private static Object loadPrimitiveType(Object config, Field field, ConfigurationSection section, String key)
      throws IllegalAccessException {
    var type = field.getType();
    if (!section.contains(key) && field.get(config) != null) {
      if (type == int.class || type == double.class || type == long.class || type == float.class) {
        return -1;
      }
      return field.get(config);
    }
    return getPrimitive(type, section, key);
  }

  private static Object loadPrimitiveType(Class<?> type, Object value) {
    if (value == null) {
      return null;
    }
    return type.cast(value);
  }

  @Nullable
  public static Object getPrimitive(Class<?> type, ConfigurationSection section, String key) {
    if (type == Integer.class || type == int.class) {
      return section.contains(key) || type == int.class ? section.getInt(key, -1) : null;
    } else if (type == Double.class || type == double.class) {
      return section.contains(key) || type == double.class ? section.getDouble(key, -1) : null;
    } else if (type == Boolean.class || type == boolean.class) {
      return section.contains(key) || type == boolean.class ? section.getBoolean(key) : null;
    } else if (type == Long.class || type == long.class) {
      return section.contains(key) || type == long.class ? section.getLong(key) : null;
    } else if (type == Float.class || type == float.class) {
      return section.contains(key) || type == float.class ? (float) section.getDouble(key, -1) : null;
    } else if (type == String.class) {
      return section.getString(key);
    } else if (type == Character.class || type == char.class) {
      String str = section.getString(key);
      return str != null && !str.isEmpty() ? str.charAt(0) : null;
    }
    return null;
  }

  private static boolean isPrimitiveOrWrapper(Class<?> type) {
    return type.isPrimitive() || type == Double.class || type == Float.class
        || type == Long.class || type == Integer.class || type == Short.class
        || type == Character.class || type == Byte.class || type == Boolean.class || type == String.class;
  }

  private static Object convertKey(Class<?> keyType, String key) {
    if (keyType == String.class) {
      return key;
    } else if (keyType == Integer.class || keyType == int.class) {
      return Integer.parseInt(key);
    } else if (keyType == Long.class || keyType == long.class) {
      return Long.parseLong(key);
    }
    return null;
  }

  private static Map<String, Object> deepMapObjectToMap(Object object) {
    Map<String, Object> map = new HashMap<>();
    Field[] fields = object.getClass().getDeclaredFields();
    for (Field field : fields) {
      field.setAccessible(true);
      var key = serializeKey(field.getName());
      try {
        if (isPrimitiveOrWrapper(field.getType())) {
          map.put(key, field.get(object));
        } else if (List.class.isAssignableFrom(field.getType())) {
          Object result = serializeCollectionToMap(field, (Collection<?>) field.get(object));
          if (result != null) {
            map.put(key, result);
          }
        } else if (Set.class.isAssignableFrom(field.getType())) {
          Object result = serializeCollectionToMap(field, (Collection<?>) field.get(object));
          if (result != null) {
            map.put(key, result);
          }
        } else if (Map.class.isAssignableFrom(field.getType())) {
          Map<?, ?> mapObject = (Map<?, ?>) field.get(object);
          Map<String, Object> serializedMap = new HashMap<>();
          for (Map.Entry<?, ?> entry : mapObject.entrySet()) {
            if (isPrimitiveOrWrapper(entry.getValue().getClass())) {
              serializedMap.put(entry.getKey().toString(), entry.getValue());
            } else {
              serializedMap.put(entry.getKey().toString(), deepMapObjectToMap(entry.getValue()));
            }
          }
          map.put(key, serializedMap);
        } else {
          map.put(key, deepMapObjectToMap(field.get(object)));
        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    return map;
  }

  private static List<Map<String, Object>> serializeComplexCollection(Collection<?> collection) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (Object elem : collection) {
      if (elem != null) {
        result.add(deepMapObjectToMap(elem));
      }
    }
    return result;
  }

  private static Object serializeCollectionToMap(Field field, Collection<?> collection) {
    if (collection == null) {
      return null;
    }
    if (!(field.getGenericType() instanceof ParameterizedType parameterizedType)) {
      return null;
    }
    if (!(parameterizedType.getActualTypeArguments()[0] instanceof Class<?> itemType)) {
      return null;
    }

    return isPrimitiveOrWrapper(itemType) ? collection : serializeComplexCollection(collection);
  }

  public static void saveObject(Object config, ConfigurationSection section) {
    Field[] fields = config.getClass().getDeclaredFields();

    for (Field field : fields) {
      if (field.isAnnotationPresent(IgnoreField.class)) {
        continue;
      }
      String key = serializeKey(field.getName());

      try {
        field.setAccessible(true);
        Object fieldValue = field.get(config);
        if (fieldValue == null) {
          continue;
        }

        if (isPrimitiveOrWrapper(field.getType())) {
          section.set(key, fieldValue);
        } else if (List.class.isAssignableFrom(field.getType())) {
          section.set(key, serializeCollectionToMap(field, (List<?>) fieldValue));
        } else if (Set.class.isAssignableFrom(field.getType())) {
          Set<?> set = (Set<?>) fieldValue;
          if (!set.isEmpty() && isPrimitiveOrWrapper(set.iterator().next().getClass())) {
            section.set(key, new ArrayList<>(set));
          } else {
            section.set(key, serializeComplexCollection(set));
          }
        } else if (Map.class.isAssignableFrom(field.getType())) {
          Map<?, ?> map = (Map<?, ?>) fieldValue;
          for (Map.Entry<?, ?> entry : map.entrySet()) {
            String mapKey = String.valueOf(entry.getKey());
            Object mapValue = entry.getValue();
            if (isPrimitiveOrWrapper(mapValue.getClass())) {
              section.set(key + "." + mapKey, mapValue);
            } else {
              ConfigurationSection mapSection = section.createSection(key + "." + mapKey);
              saveObject(mapValue, mapSection);
            }
          }
        } else if (ConfigurationSection.class.isAssignableFrom(field.getType())) {
          section.set(key, fieldValue);
        } else {
          ConfigurationSection nestedSection = section.createSection(key);
          saveObject(fieldValue, nestedSection);
        }

      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
  }

  public static void save(Object config, YamlConfiguration yaml, File file) {
    saveObject(config, yaml);

    try {
      yaml.save(file);
    } catch (IOException e) {
      OxLib.getPlugin(OxLib.class).getLogger().warning("Failed to save " + file.getAbsolutePath());
    }
  }

}
