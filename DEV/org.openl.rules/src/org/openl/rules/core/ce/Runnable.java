package org.openl.rules.core.ce;

import org.openl.vm.IRuntimeEnv;

@FunctionalInterface
public interface Runnable {
    void run(IRuntimeEnv env);
}
