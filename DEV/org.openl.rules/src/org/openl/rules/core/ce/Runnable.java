package org.openl.rules.core.ce;

import org.openl.vm.IRuntimeEnv;

@FunctionalInterface
public interface Runnable {
    public void run(IRuntimeEnv env);
}
