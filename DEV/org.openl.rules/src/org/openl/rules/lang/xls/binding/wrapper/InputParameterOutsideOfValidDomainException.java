package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.binding.impl.cast.OutsideOfValidDomainException;

public class InputParameterOutsideOfValidDomainException extends OutsideOfValidDomainException{

    private static final long serialVersionUID = -7450964397989796059L;

    public InputParameterOutsideOfValidDomainException() {
        super();
    }

    public InputParameterOutsideOfValidDomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public InputParameterOutsideOfValidDomainException(String message) {
        super(message);
    }

    public InputParameterOutsideOfValidDomainException(Throwable cause) {
        super(cause);
    }

}
