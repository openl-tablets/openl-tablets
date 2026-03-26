package org.openl.rules.project.model.validation;

import java.io.Serial;

public class ValidationException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }

}
