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

import org.openl.exception.OpenlNotCheckedException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public class ObjectMapperConfigurationParsingException extends OpenlNotCheckedException {
    public ObjectMapperConfigurationParsingException() {
    }

    public ObjectMapperConfigurationParsingException(String message) {
        super(message);
    }

    public ObjectMapperConfigurationParsingException(Throwable cause) {
        super(cause);
    }

    public ObjectMapperConfigurationParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ObjectMapperConfigurationParsingException(String message,
            Throwable cause,
            ILocation location,
            IOpenSourceCodeModule sourceModule) {
        super(message, cause, location, sourceModule);
    }
}
