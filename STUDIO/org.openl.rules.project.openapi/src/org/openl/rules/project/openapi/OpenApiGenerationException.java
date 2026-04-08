package org.openl.rules.project.openapi;

import java.io.Serial;

public class OpenApiGenerationException extends Exception {

    @Serial
    private static final long serialVersionUID = 4688639540320312954L;

    public OpenApiGenerationException(String message) {
        super(message);
    }

    public OpenApiGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
