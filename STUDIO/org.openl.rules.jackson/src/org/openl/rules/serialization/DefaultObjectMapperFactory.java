package org.openl.rules.serialization;

/*-
 * #%L
 * OpenL - STUDIO - Jackson
 * %%
 * Copyright (C) 2016 - 2020 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

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
