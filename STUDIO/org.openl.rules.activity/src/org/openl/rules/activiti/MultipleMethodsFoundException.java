package org.openl.rules.activiti;

import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;

public class MultipleMethodsFoundException extends OpenLRuntimeException{

    private static final long serialVersionUID = -2134177718752351064L;

    public MultipleMethodsFoundException() {
        super();
    }

    public MultipleMethodsFoundException(String message, IBoundNode node) {
        super(message, node);
    }

    public MultipleMethodsFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public MultipleMethodsFoundException(String message) {
        super(message);
    }

    public MultipleMethodsFoundException(Throwable cause, IBoundNode node) {
        super(cause, node);
    }

    public MultipleMethodsFoundException(Throwable cause) {
        super(cause);
    }
    
}

