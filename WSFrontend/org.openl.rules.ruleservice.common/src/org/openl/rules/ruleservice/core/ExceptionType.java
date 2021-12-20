package org.openl.rules.ruleservice.core;

public enum ExceptionType {
    USER_ERROR,
    RULES_RUNTIME,
    COMPILATION,
    SYSTEM,
    BAD_REQUEST,
    VALIDATION;

    public static boolean isServerError(ExceptionType type) {
        return type == SYSTEM || type == COMPILATION || type == RULES_RUNTIME;
    }
}
