package org.openl.rules.activiti;

import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;

public class MethodNotFoundException extends OpenLRuntimeException{

    private static final long serialVersionUID = 9018051982378241633L;

    public MethodNotFoundException() {
        super();
    }

    public MethodNotFoundException(String message, IBoundNode node) {
        super(message, node);
    }

    public MethodNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public MethodNotFoundException(String message) {
        super(message);
    }

    public MethodNotFoundException(Throwable cause, IBoundNode node) {
        super(cause, node);
    }

    public MethodNotFoundException(Throwable cause) {
        super(cause);
    }
    
}
