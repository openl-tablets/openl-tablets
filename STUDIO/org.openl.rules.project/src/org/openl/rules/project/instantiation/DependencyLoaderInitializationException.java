package org.openl.rules.project.instantiation;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public class DependencyLoaderInitializationException extends OpenlNotCheckedException {

    private static final long serialVersionUID = 1L;

    public DependencyLoaderInitializationException() {
        super();
    }

    public DependencyLoaderInitializationException(String message,
            Throwable cause,
            ILocation location,
            IOpenSourceCodeModule sourceModule) {
        super(message, cause, location, sourceModule);
    }

    public DependencyLoaderInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DependencyLoaderInitializationException(String message) {
        super(message);
    }

    public DependencyLoaderInitializationException(Throwable cause) {
        super(cause);
    }

}
