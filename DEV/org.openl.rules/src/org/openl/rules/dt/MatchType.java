package org.openl.rules.dt;

enum MatchType {
    STRICT(0),
    STRICT_LOCAL_PARAMS_RENAMED(1),
    STRICT_CASTED(2),
    STRICT_CASTED_LOCAL_PARAMS_RENAMED(3),
    METHOD_PARAMS_RENAMED(4),
    METHOD_LOCAL_PARAMS_RENAMED(5),
    LOCAL_PARAMS_RENAMED_CASTED(6),
    METHOD_PARAMS_RENAMED_CASTED(7),
    METHOD_LOCAL_PARAMS_RENAMED_CASTED(8);

    int priority;

    private MatchType(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
