package org.openl.rules.dt;

enum MatchType {
    STRICT(0),
    STRICT_PARAMS_RENAMED(1),
    STRICT_CASTED(2),
    STRICT_CASTED_PARAMS_RENAMED(3),
    METHOD_ARGS_RENAMED(4),
    METHOD_ARGS_AND_PARAMS_RENAMED(5),
    PARAMS_RENAMED_CASTED(6),
    METHOD_ARGS_RENAMED_CASTED(7),
    METHOD_ARGS_AND_PARAMS_RENAMED_CASTED(8);

    final int priority;

    MatchType(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
