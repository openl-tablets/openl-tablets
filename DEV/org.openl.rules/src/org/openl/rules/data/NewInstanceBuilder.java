package org.openl.rules.data;

import org.openl.vm.IRuntimeEnv;

public interface NewInstanceBuilder {
    Object newInstance(IRuntimeEnv env);
}
