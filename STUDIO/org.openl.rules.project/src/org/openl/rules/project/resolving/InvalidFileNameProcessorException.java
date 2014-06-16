package org.openl.rules.project.resolving;

public class InvalidFileNameProcessorException extends Exception {
    private static final long serialVersionUID = 4810526176549395179L;

    public InvalidFileNameProcessorException() {
    }

    public InvalidFileNameProcessorException(String message) {
        super(message);
    }

    public InvalidFileNameProcessorException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFileNameProcessorException(Throwable cause) {
        super(cause);
    }
}
