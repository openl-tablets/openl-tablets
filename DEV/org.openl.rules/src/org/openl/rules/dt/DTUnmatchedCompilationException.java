package org.openl.rules.dt;

import org.openl.exception.OpenLCompilationException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public class DTUnmatchedCompilationException extends OpenLCompilationException {
    public DTUnmatchedCompilationException(String message,
            Throwable insideCause,
            ILocation location,
            IOpenSourceCodeModule source) {
        super(message, insideCause, location, source);
    }

    public DTUnmatchedCompilationException(String message, Throwable cause, ILocation location) {
        super(message, cause, location);
    }

    public DTUnmatchedCompilationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DTUnmatchedCompilationException(String message) {
        super(message);
    }
}
