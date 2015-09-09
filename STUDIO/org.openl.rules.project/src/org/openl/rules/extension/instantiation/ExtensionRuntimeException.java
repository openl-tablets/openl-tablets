package org.openl.rules.extension.instantiation;

import org.openl.exception.OpenLRuntimeException;

public class ExtensionRuntimeException extends OpenLRuntimeException {
    public ExtensionRuntimeException(String message) {
        super(message);
    }
}
