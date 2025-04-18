package org.openl.rules.ruleservice.core;

public enum ExceptionType {
    USER_ERROR,
    RULES_RUNTIME,
    COMPILATION,
    SYSTEM,
    BAD_REQUEST,
    VALIDATION;

    public boolean isServerError() {
        return this == SYSTEM || this == COMPILATION || this == RULES_RUNTIME;
    }
}
