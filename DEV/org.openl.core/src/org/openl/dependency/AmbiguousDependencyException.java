package org.openl.dependency;

import org.openl.exception.OpenLCompilationException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public class AmbiguousDependencyException extends OpenLCompilationException {
    public AmbiguousDependencyException(String message,
            Throwable insideCause,
            ILocation location,
            IOpenSourceCodeModule source) {
        super(message, insideCause, location, source);
    }

    public AmbiguousDependencyException(String message, Throwable cause, ILocation location) {
        super(message, cause, location);
    }

    public AmbiguousDependencyException(String message, Throwable cause) {
        super(message, cause);
    }

    public AmbiguousDependencyException(String message) {
        super(message);
    }
}
