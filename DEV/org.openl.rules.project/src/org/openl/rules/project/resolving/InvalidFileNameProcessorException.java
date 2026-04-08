package org.openl.rules.project.resolving;

import java.io.Serial;

public class InvalidFileNameProcessorException extends Exception {
    @Serial
    private static final long serialVersionUID = 4810526176549395179L;

    InvalidFileNameProcessorException(String message) {
        super(message);
    }

    InvalidFileNameProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
