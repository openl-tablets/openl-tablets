package org.openl.rules.dt;

enum MatchType {
    STRICT(0),
    STRICT_LOCAL_PARAMS_RENAMED(1),
    METHOD_PARAMS_RENAMED(2),
    METHOD_LOCAL_PARAMS_RENAMED(3),
    CASTED(4),
    LOCAL_PARAMS_RENAMED_CASTED(5),
    METHOD_PARAMS_RENAMED_CASTED(6),
    METHOD_LOCAL_PARAMS_RENAMED_CASTED(7);

    int priority;

    private MatchType(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
