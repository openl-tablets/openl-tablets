package org.openl.rules.project.resolving;

public class InvalidFileNameProcessorException extends Exception {
    private static final long serialVersionUID = 4810526176549395179L;

    InvalidFileNameProcessorException(String message) {
        super(message);
    }

    InvalidFileNameProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
