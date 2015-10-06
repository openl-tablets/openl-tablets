package org.openl.rules.activiti;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public class ResourceCompileException extends OpenlNotCheckedException{

    private static final long serialVersionUID = -8353037823013785531L;

    public ResourceCompileException() {
        super();
    }

    public ResourceCompileException(String message,
            Throwable cause,
            ILocation location,
            IOpenSourceCodeModule sourceModule) {
        super(message, cause, location, sourceModule);
    }

    public ResourceCompileException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceCompileException(String message) {
        super(message);
    }

    public ResourceCompileException(Throwable cause) {
        super(cause);
    }

}
