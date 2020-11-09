package org.openl.rules.ruleservice.storelogdata.hive;

import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;

public class HiveTableCreationException extends RuleServiceRuntimeException {

    private static final long serialVersionUID = -4405787184878140722L;

    public HiveTableCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public HiveTableCreationException(String message) {
        super(message);
    }

}
