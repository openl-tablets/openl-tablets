package org.openl.rules.project.openapi;

public class OpenApiGenerationException extends Exception {

    private static final long serialVersionUID = 4688639540320312954L;

    public OpenApiGenerationException(String message) {
        super(message);
    }

    public OpenApiGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
