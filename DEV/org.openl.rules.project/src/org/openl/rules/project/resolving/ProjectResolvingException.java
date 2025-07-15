package org.openl.rules.project.resolving;

import org.openl.exception.OpenLCheckedException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public class ProjectResolvingException extends OpenLCheckedException {

    private static final long serialVersionUID = 1L;

    public ProjectResolvingException() {
        super();
    }

    public ProjectResolvingException(String message,
                                     Throwable cause,
                                     ILocation location,
                                     IOpenSourceCodeModule sourceModule) {
        super(message, cause, location, sourceModule);
    }

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
