package org.openl.rules.activiti;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public class ResourcePrepareException extends OpenlNotCheckedException{

    private static final long serialVersionUID = 4221185497893056680L;

    public ResourcePrepareException() {
        super();
    }

    public ResourcePrepareException(String message,
            Throwable cause,
            ILocation location,
            IOpenSourceCodeModule sourceModule) {
        super(message, cause, location, sourceModule);
    }

    public ResourcePrepareException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourcePrepareException(String message) {
        super(message);
    }

    public ResourcePrepareException(Throwable cause) {
        super(cause);
    }

}
