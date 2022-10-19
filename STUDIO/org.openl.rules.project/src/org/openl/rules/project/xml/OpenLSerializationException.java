package org.openl.rules.project.xml;

import org.openl.exception.OpenlNotCheckedException;

public class OpenLSerializationException extends OpenlNotCheckedException {
    public OpenLSerializationException(String message) {
        super(message);
    }

    public OpenLSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
