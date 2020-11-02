package org.openl.rules.ruleservice.storelogdata.hive;

import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;

public class TableCreationException extends RuleServiceRuntimeException {

    private static final long serialVersionUID = -4405787184878140722L;

    public TableCreationException() {
        super();
    }

    public TableCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TableCreationException(String message) {
        super(message);
    }

    public TableCreationException(Throwable cause) {
        super(cause);
    }

}
