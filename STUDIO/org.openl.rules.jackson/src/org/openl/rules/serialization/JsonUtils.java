package org.openl.rules.serialization;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtils {
    private static final WeakHashMap<Object, ObjectMapper> cache = new WeakHashMap<>();
    private final static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static volatile ObjectMapper defaultObjectMapper;

    private JsonUtils() {
    }

    public static ObjectMapper createJacksonObjectMapper(Class<?>[] types, boolean enableDefaultTyping) {
        if (enableDefaultTyping) {
            return createJacksonObjectMapper(types, DefaultTypingMode.EVERYTHING);
        } else {
            return createJacksonObjectMapper(types, DefaultTypingMode.JAVA_LANG_OBJECT);
        }
    }

    public static ObjectMapper createJacksonObjectMapper(Class<?>[] types, DefaultTypingMode defaultTypingMode) {
        JacksonObjectMapperFactoryBean jacksonObjectMapperFactoryBean = new JacksonObjectMapperFactoryBean();
        jacksonObjectMapperFactoryBean.setDefaultTypingMode(defaultTypingMode);
        jacksonObjectMapperFactoryBean.setSupportVariations(true);
        jacksonObjectMapperFactoryBean.setOverrideClasses(new HashSet<>(Arrays.asList(types)));
        try {
            return jacksonObjectMapperFactoryBean.createJacksonObjectMapper();
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static ObjectMapper getDefaultJacksonObjectMapper() {
        JacksonObjectMapperFactoryBean jacksonObjectMapperFactoryBean = new JacksonObjectMapperFactoryBean();
        jacksonObjectMapperFactoryBean.setSupportVariations(true);
        try {
            return jacksonObjectMapperFactoryBean.createJacksonObjectMapper();
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }

    /**
     * Cached objectMapper for set of classes
     *
     * @param classes set of classes
     * @return objectMapper
     */
    public static ObjectMapper getCachedObjectMapper(Object key, Class<?>[] classes) {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lock();
            ObjectMapper objectMapper = cache.get(key);
            if (objectMapper != null) return objectMapper;
        } finally {
            readLock.unlock();
        }
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lock();
            ObjectMapper objectMapper = cache.get(key);
            if (objectMapper == null) {
                objectMapper = JsonUtils.createJacksonObjectMapper(classes, false);
                cache.put(key, objectMapper);
            }
            return objectMapper;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Singleton object mapper
     *
     * @return objectMapper
     */
    public static ObjectMapper getDefaultObjectMapper() {
        if (defaultObjectMapper == null) {
            synchronized (JsonUtils.class) {
                if (defaultObjectMapper == null) {
                    defaultObjectMapper = getDefaultJacksonObjectMapper();
                }
            }
        }
        return defaultObjectMapper;
    }

    public static String toJSON(Object value) throws JsonProcessingException {
        return getDefaultJacksonObjectMapper().writeValueAsString(value);
    }

    public static String toJSON(Object value, ObjectMapper objectMapper) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    public static String toJSON(Object value, Class<?>[] types) throws JsonProcessingException {
        return toJSON(value, types, false);
    }

    public static String toJSON(Object value,
            Class<?>[] types,
            boolean enableDefaultTyping) throws JsonProcessingException {
        if (types == null) {
            types = new Class<?>[0];
        }
        ObjectMapper objectMapper = createJacksonObjectMapper(types, enableDefaultTyping);
        return objectMapper.writeValueAsString(value);
    }

    public static Map<String, String> splitJSON(String jsonString) throws IOException {
        ObjectMapper objectMapper = getDefaultJacksonObjectMapper();
        return splitJSON(jsonString, objectMapper);
    }

    public static Map<String, String> splitJSON(String jsonString, ObjectMapper objectMapper) throws IOException {
        JsonNode rootNode = objectMapper.readTree(jsonString);
        Map<String, String> splitMap = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = rootNode.fields();
        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JsonNode> field = fieldsIterator.next();
            splitMap.put(field.getKey(), objectMapper.writeValueAsString(field.getValue()));
        }
        return splitMap;
    }

    public static <T> T fromJSON(String jsonString, Class<T> readType, ObjectMapper objectMapper) throws IOException {
        return objectMapper.readValue(jsonString, readType);
    }

    public static <T> T fromJSON(String jsonString, Class<T> readType) throws IOException {
        return getDefaultJacksonObjectMapper().readValue(jsonString, readType);
    }

    public static <T> T fromJSON(String jsonString, Class<T> readType, Class<?>[] types) throws IOException {
        if (types == null) {
            types = new Class<?>[0];
        }
        ObjectMapper objectMapper = createJacksonObjectMapper(types, DefaultTypingMode.DISABLED);
        return objectMapper.readValue(jsonString, readType);
    }

    @Deprecated
    public static <T> T fromJSON(String jsonString,
            Class<T> readType,
            Class<?>[] types,
            boolean enableDefaultTyping) throws IOException {
        return fromJSON(jsonString, readType, types);
    }
}
