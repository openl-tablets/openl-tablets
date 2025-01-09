package org.openl.rules.vm;

import org.openl.vm.SimpleRuntimeEnv;
import org.openl.vm.SimpleVM;

public class SimpleRulesVM extends SimpleVM {

    @Override
    public SimpleRuntimeEnv getRuntimeEnv() {
        return new SimpleRuntimeEnv();
    }

}
