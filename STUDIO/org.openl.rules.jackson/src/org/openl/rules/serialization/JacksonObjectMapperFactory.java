package org.openl.rules.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface JacksonObjectMapperFactory {
    ObjectMapper createJacksonObjectMapper() throws ClassNotFoundException;
}
