package org.openl.rules.serialization;

import org.openl.exception.OpenlNotCheckedException;

public class ObjectMapperConfigurationParsingException extends OpenlNotCheckedException {

    public ObjectMapperConfigurationParsingException(String message) {
        super(message);
    }

    public ObjectMapperConfigurationParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
