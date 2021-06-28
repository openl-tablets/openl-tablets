package org.openl.rules.serialization;

import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMapperCache {

    private static final WeakHashMap<List<Class<?>>, ObjectMapper> cache = new WeakHashMap<>();
    private static volatile ObjectMapper defaultObjectMapper;

    /**
     * ObjectMapper for set of classes
     *
     * @param classes set of classes
     * @return objectMapper
     */
    public static ObjectMapper getObjectMapper(Class<?>[] classes) {
        List<Class<?>> key = Arrays.asList(classes);
        if (!cache.containsKey(key)) {
            synchronized (cache) {
                if (!cache.containsKey(key)) {
                    ObjectMapper objectMapper = JsonUtils.createJacksonObjectMapper(classes, false);
                    cache.put(key, objectMapper);
                }
            }
        }
        return cache.get(key);
    }

    /**
     * Default object mapper
     *
     * @return objectMapper
     */
    public static ObjectMapper getDefaultObjectMapper() {
        if (defaultObjectMapper == null) {
            synchronized (ObjectMapperCache.class) {
                if (defaultObjectMapper == null) {
                    defaultObjectMapper = JsonUtils.getDefaultJacksonObjectMapper();
                }
            }
        }
        return defaultObjectMapper;
    }

}
