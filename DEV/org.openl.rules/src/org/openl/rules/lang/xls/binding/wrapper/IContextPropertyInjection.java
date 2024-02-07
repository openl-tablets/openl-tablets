package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.vm.IRuntimeEnv;

interface IContextPropertyInjection {
    IRulesRuntimeContext inject(Object[] params,
                                IRuntimeEnv env,
                                SimpleRulesRuntimeEnv simpleRulesRuntimeEnv,
                                IRulesRuntimeContext rulesRuntimeContext);

}
