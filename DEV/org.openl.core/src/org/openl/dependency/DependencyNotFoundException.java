package org.openl.dependency;

import org.openl.exception.OpenLCompilationException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public class DependencyNotFoundException extends OpenLCompilationException {
    public DependencyNotFoundException(String message,
            Throwable insideCause,
            ILocation location,
            IOpenSourceCodeModule source) {
        super(message, insideCause, location, source);
    }

    public DependencyNotFoundException(String message, Throwable cause, ILocation location) {
        super(message, cause, location);
    }

    public DependencyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public DependencyNotFoundException(String message) {
        super(message);
    }
}
