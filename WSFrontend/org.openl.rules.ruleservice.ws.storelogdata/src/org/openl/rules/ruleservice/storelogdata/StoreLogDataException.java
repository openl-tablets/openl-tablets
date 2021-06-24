package org.openl.rules.ruleservice.storelogdata;

import org.openl.rules.ruleservice.core.RuleServiceException;

public class StoreLogDataException extends RuleServiceException {
    public StoreLogDataException() {
    }

    public StoreLogDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public StoreLogDataException(String message) {
        super(message);
    }

    public StoreLogDataException(Throwable cause) {
        super(cause);
    }
}
