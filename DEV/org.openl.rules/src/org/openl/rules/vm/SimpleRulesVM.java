package org.openl.rules.vm;

import org.openl.vm.SimpleVM;

public class SimpleRulesVM extends SimpleVM {

    @Override
    public SimpleRulesRuntimeEnv getRuntimeEnv() {
        return new SimpleRulesRuntimeEnv();
    }

}
