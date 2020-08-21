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

public interface JacksonObjectMapperFactory {
    ObjectMapper createJacksonObjectMapper() throws ClassNotFoundException;
}
