package org.openl.rules.project.instantiation;

import java.io.Serial;

import org.openl.exception.OpenlNotCheckedException;

public class DependencyLoaderInitializationException extends OpenlNotCheckedException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DependencyLoaderInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

}
