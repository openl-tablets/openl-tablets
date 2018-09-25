package org.openl.rules.ruleservice.logging.cassandra;

import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;

public class SchemaCreationException extends RuleServiceRuntimeException {

    private static final long serialVersionUID = -4405787184878140722L;

    public SchemaCreationException() {
        super();
    }

    public SchemaCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SchemaCreationException(String message) {
        super(message);
    }

    public SchemaCreationException(Throwable cause) {
        super(cause);
    }

}
