package org.openl.rules.serialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * #%L
 * OpenL - Rules - Serialization
 * %%
 * Copyright (C) 2016 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

public final class JsonUtils {
    public static final class DefaultObjectMapperHolder {
        private static final ObjectMapper INSTANCE;
        static {
            JacksonObjectMapperFactoryBean jacksonObjectMapperFactoryBean = new JacksonObjectMapperFactoryBean();
            jacksonObjectMapperFactoryBean.setDefaultTypingType(DefaultTypingType.SMART);
            jacksonObjectMapperFactoryBean.setSupportVariations(true);
            INSTANCE = jacksonObjectMapperFactoryBean.createJacksonObjectMapper();
        }
    }
    
    public static ObjectMapper createJacksonObjectMapper(Class<?>[] types, boolean enableDefaultTyping) {
        if (enableDefaultTyping) {
            return createJacksonObjectMapper(types, DefaultTypingType.ENABLE);
        } else {
            return createJacksonObjectMapper(types, DefaultTypingType.SMART);
        }
    }

    public static ObjectMapper createJacksonObjectMapper(Class<?>[] types, DefaultTypingType defaultTypingType) {
        JacksonObjectMapperFactoryBean jacksonObjectMapperFactoryBean = new JacksonObjectMapperFactoryBean();
        jacksonObjectMapperFactoryBean.setDefaultTypingType(defaultTypingType);
        jacksonObjectMapperFactoryBean.setSupportVariations(true);
        Set<String> overideTypes = new HashSet<String>();
        for (Class<?> type : types) {
            overideTypes.add(type.getCanonicalName());
        }
        jacksonObjectMapperFactoryBean.setOverrideTypes(overideTypes);
        return jacksonObjectMapperFactoryBean.createJacksonObjectMapper();
    }

    private static ObjectMapper getDefaultJacksonObjectMapper() {
        return DefaultObjectMapperHolder.INSTANCE;
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

    public static Map<String, String> splitJSON(String jsonString) throws IOException, JsonProcessingException {
        ObjectMapper objectMapper = getDefaultJacksonObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonString);
        Map<String, String> splitMap = new HashMap<String, String>();
        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = rootNode.fields();
        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JsonNode> field = fieldsIterator.next();
            splitMap.put(field.getKey(), objectMapper.writeValueAsString(field.getValue()));
        }
        return splitMap;
    }

    public static <T> T fromJSON(String jsonString, Class<T> readType, ObjectMapper objectMapper) throws IOException, JsonProcessingException {
        return objectMapper.readValue(jsonString, readType);
    }

    public static <T> T fromJSON(String jsonString, Class<T> readType) throws IOException, JsonProcessingException {
        return getDefaultJacksonObjectMapper().readValue(jsonString, readType);
    }
    
    public static <T> T fromJSON(String jsonString, Class<T> readType, Class<?>[] types) throws IOException,
                                                                                         JsonProcessingException {
        return fromJSON(jsonString, readType, types, false);
    }

    public static <T> T fromJSON(String jsonString,
            Class<T> readType,
            Class<?>[] types,
            boolean enableDefaultTyping) throws IOException, JsonProcessingException {
        if (types == null) {
            types = new Class<?>[0];
        }
        ObjectMapper objectMapper = createJacksonObjectMapper(types, enableDefaultTyping);
        return objectMapper.readValue(jsonString, readType);
    }
}
