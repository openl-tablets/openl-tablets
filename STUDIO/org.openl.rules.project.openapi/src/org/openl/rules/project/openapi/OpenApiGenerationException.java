package org.openl.rules.project.openapi;

import org.openl.rules.project.instantiation.RulesInstantiationException;

public class OpenApiGenerationException extends RulesInstantiationException {

    private static final long serialVersionUID = 4688639540320312954L;

    public OpenApiGenerationException(String message) {
        super(message);
    }

    public OpenApiGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
