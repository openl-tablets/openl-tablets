package org.openl.rules.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;

final class DefaultObjectMapperFactory implements ObjectMapperFactory {

    static class DefaultObjectMapperFactoryHolder {
        static DefaultObjectMapperFactory INSTANCE = new DefaultObjectMapperFactory();
    }

    private DefaultObjectMapperFactory() {
    }

    static ObjectMapperFactory getInstance() {
        return DefaultObjectMapperFactoryHolder.INSTANCE;
    }

    @Override
    public ObjectMapper createObjectMapper() {
        return new ObjectMapper();
    }
}
