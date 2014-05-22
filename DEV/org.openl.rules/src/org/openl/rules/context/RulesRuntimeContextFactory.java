package org.openl.rules.context;

public class RulesRuntimeContextFactory {
    public static IRulesRuntimeContext buildRulesRuntimeContext() {
        return new DefaultRulesRuntimeContext();
    }
}
