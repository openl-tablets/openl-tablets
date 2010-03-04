package org.openl.rules.cmatch.algorithm;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.vm.IRuntimeEnv;

public interface IMatchAlgorithmExecutor {
    Object invoke(Object target, Object[] params, IRuntimeEnv env, ColumnMatch columnMatch);
}
