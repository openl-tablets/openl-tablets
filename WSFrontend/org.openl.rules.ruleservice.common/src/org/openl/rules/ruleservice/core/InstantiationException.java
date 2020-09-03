package org.openl.rules.ruleservice.core;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public class InstantiationException extends OpenlNotCheckedException {
    public InstantiationException() {
    }

    public InstantiationException(String message) {
        super(message);
    }

    public InstantiationException(Throwable cause) {
        super(cause);
    }

    public InstantiationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InstantiationException(String message,
            Throwable cause,
            ILocation location,
            IOpenSourceCodeModule sourceModule) {
        super(message, cause, location, sourceModule);
    }
}
