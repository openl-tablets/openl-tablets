package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.vm.SimpleRuntimeEnv;
import org.openl.vm.IRuntimeEnv;

interface IContextPropertyInjection {
    IRulesRuntimeContext inject(Object[] params,
                                IRuntimeEnv env,
                                SimpleRuntimeEnv simpleRuntimeEnv,
                                IRulesRuntimeContext rulesRuntimeContext);

}
