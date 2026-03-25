package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleRuntimeEnv;

interface IContextPropertyInjection {
    IRulesRuntimeContext inject(Object[] params,
                                IRuntimeEnv env,
                                SimpleRuntimeEnv simpleRuntimeEnv,
                                IRulesRuntimeContext rulesRuntimeContext);

}
