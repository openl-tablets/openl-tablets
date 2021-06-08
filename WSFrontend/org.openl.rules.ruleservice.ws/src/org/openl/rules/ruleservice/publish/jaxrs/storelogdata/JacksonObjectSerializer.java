package org.openl.rules.ruleservice.publish.jaxrs.storelogdata;

import org.openl.rules.ruleservice.storelogdata.ObjectSerializer;
import org.openl.rules.ruleservice.storelogdata.ProcessingException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonObjectSerializer implements ObjectSerializer {

    private final ObjectMapper objectMapper;

    public JacksonObjectSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T readValue(String content, Class<T> type) throws ProcessingException {
        try {
            return objectMapper.readValue(content, type);
        } catch (JsonProcessingException e) {
            throw new ProcessingException(e);
        }
    }

    @Override
    public String writeValueAsString(Object obj) throws ProcessingException {
        if (obj instanceof String) {
            return (String) obj;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new ProcessingException(e);
        }
    }
}
