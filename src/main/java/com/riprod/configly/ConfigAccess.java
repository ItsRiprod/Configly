package com.riprod.configly;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.JsonAsset;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ConfigAccess {
    private static final Map<Class<?>, Field> ID_FIELDS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Field> DATA_FIELDS = new ConcurrentHashMap<>();

    private ConfigAccess() {
    }

    static Object getId(Object config) {
        return ((JsonAsset<?>) config).getId();
    }

    static void setId(Object config, Object id) {
        write(ID_FIELDS, config, "id", id);
    }

    static Object getData(Object config) {
        return read(DATA_FIELDS, config, "data");
    }

    static void setData(Object config, Object data) {
        write(DATA_FIELDS, config, "data", data);
    }

    private static Object read(Map<Class<?>, Field> cache, Object config, String name) {
        try {
            return field(cache, config.getClass(), name).get(config);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not read '" + name + "' on " + config.getClass().getName(), e);
        }
    }

    private static void write(Map<Class<?>, Field> cache, Object config, String name, Object value) {
        try {
            field(cache, config.getClass(), name).set(config, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not write '" + name + "' on " + config.getClass().getName(), e);
        }
    }

    private static Field field(Map<Class<?>, Field> cache, Class<?> type, String name) {
        return cache.computeIfAbsent(type, key -> {
            for (var current = key; current != null; current = current.getSuperclass()) {
                try {
                    var field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                    // declared further up the hierarchy
                }
            }
            throw new IllegalStateException(key.getName() + " does not extend ConfiglyConfig: no '" + name + "' field");
        });
    }
}
