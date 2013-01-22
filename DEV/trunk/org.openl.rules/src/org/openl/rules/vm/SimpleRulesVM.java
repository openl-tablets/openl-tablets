package org.openl.rules.vm;

import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

public class SimpleRulesVM extends SimpleVM {
    @Override
    public IRuntimeEnv getRuntimeEnv() {
        return new SimpleRulesRuntimeEnv();
    }
}
