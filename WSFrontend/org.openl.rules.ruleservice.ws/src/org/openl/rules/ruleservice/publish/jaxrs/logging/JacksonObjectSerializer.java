package org.openl.rules.ruleservice.publish.jaxrs.logging;

import org.openl.rules.ruleservice.logging.ObjectSerializer;
import org.openl.rules.ruleservice.logging.ProcessingException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonObjectSerializer implements ObjectSerializer {

    private ObjectMapper objectMapper;

    public JacksonObjectSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String writeValueAsString(Object obj) throws ProcessingException {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new ProcessingException(e);
        }
    }
}
