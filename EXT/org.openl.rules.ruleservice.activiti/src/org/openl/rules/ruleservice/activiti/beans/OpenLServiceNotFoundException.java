package org.openl.rules.ruleservice.activiti.beans;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public class OpenLServiceNotFoundException extends OpenlNotCheckedException {

    private static final long serialVersionUID = -2134177718752351064L;

    public OpenLServiceNotFoundException() {
        super();
    }

    public OpenLServiceNotFoundException(String message,
            Throwable cause,
            ILocation location,
            IOpenSourceCodeModule sourceModule) {
        super(message, cause, location, sourceModule);
    }

    public OpenLServiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public OpenLServiceNotFoundException(String message) {
        super(message);
    }

    public OpenLServiceNotFoundException(Throwable cause) {
        super(cause);
    }

}
