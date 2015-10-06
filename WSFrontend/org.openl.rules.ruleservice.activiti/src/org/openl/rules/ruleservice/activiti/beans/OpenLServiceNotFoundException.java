package org.openl.rules.ruleservice.activiti.beans;

import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;

public class OpenLServiceNotFoundException extends OpenLRuntimeException {

    private static final long serialVersionUID = -2134177718752351064L;

    public OpenLServiceNotFoundException() {
        super();
    }

    public OpenLServiceNotFoundException(String message, IBoundNode node) {
        super(message, node);
    }

    public OpenLServiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public OpenLServiceNotFoundException(String message) {
        super(message);
    }

    public OpenLServiceNotFoundException(Throwable cause, IBoundNode node) {
        super(cause, node);
    }

    public OpenLServiceNotFoundException(Throwable cause) {
        super(cause);
    }

}
