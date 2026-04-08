package org.openl.rules.ruleservice.simple;

import java.io.Serial;

public class MethodInvocationRuntimeException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public MethodInvocationRuntimeException(Throwable cause) {
        super(cause);
    }

}
