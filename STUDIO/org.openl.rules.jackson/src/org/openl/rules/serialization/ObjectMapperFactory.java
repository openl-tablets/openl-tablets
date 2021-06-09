package org.openl.rules.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface ObjectMapperFactory {
    ObjectMapper createObjectMapper();
}
