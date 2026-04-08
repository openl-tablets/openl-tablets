package org.openl.rules.project.instantiation;

import java.io.Serial;

public class ValidationServiceClassException extends RulesInstantiationException {
    @Serial
    private static final long serialVersionUID = -6757621309307573119L;

    public ValidationServiceClassException(String message) {
        super(message);
    }

    public ValidationServiceClassException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValidationServiceClassException(Throwable cause) {
        super(cause);
    }
}
