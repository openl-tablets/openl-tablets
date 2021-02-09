package org.openl.rules.serialization;

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
