package org.openl.rules.project.resolving;

import java.io.Serial;

import org.openl.exception.OpenLCheckedException;

public class ProjectResolvingException extends OpenLCheckedException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ProjectResolvingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProjectResolvingException(String message) {
        super(message);
    }

    public ProjectResolvingException(Throwable cause) {
        super(cause);
    }

}
