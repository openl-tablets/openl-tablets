package org.openl.rules.serialization;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
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

public final class OpenLObjectToJSONUtils {
    public static final class ObjectMapperHolder {
        private static final ObjectMapper INSTANCE;
        static {
            JacksonObjectMapperFactoryBean jacksonObjectMapperFactoryBean = new JacksonObjectMapperFactoryBean();
            jacksonObjectMapperFactoryBean.setEnableDefaultTyping(true);
            jacksonObjectMapperFactoryBean.setSupportVariations(true);
            INSTANCE = jacksonObjectMapperFactoryBean.createJacksonObjectMapper();
        }
    }

    public static ObjectMapper getJacksonObjectMapper() {
        return ObjectMapperHolder.INSTANCE;
    }

    public static String serialize(Object value) throws JsonProcessingException {
        return getJacksonObjectMapper().writeValueAsString(value);
    }

    public static Object deserialize(File inputFile, Class<?> readType) throws IOException, JsonProcessingException {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(inputFile);
            return getJacksonObjectMapper().readValue(new FileInputStream(inputFile), readType);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    public static Object deserialize(String content, Class<?> readType) throws IOException, JsonProcessingException {
        return getJacksonObjectMapper().readValue(content, readType);
    }
}
