package org.openl.rules.helpers;

import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.JavaNoCast;
import org.openl.binding.impl.cast.MethodCallerWrapper;
import org.openl.binding.impl.cast.MethodDetails;
import org.openl.binding.impl.cast.MethodDetailsMethodCaller;
import org.openl.binding.impl.method.AutoCastableResultOpenMethod;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.vm.SimpleRulesRuntimeEnv;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * Implementation of {@link MethodCallerWrapper} for copy method from {@link RulesUtils}.
 */
public class CopyMethodCallerWrapper implements MethodCallerWrapper {

    private MethodDetails buildMethodDetails(IRuntimeEnv env) {
        if (env instanceof SimpleRulesRuntimeEnv) {
            SimpleRulesRuntimeEnv simpleRulesRuntimeEnv = (SimpleRulesRuntimeEnv) env;
            return new CopyMethodDetails(((XlsModuleOpenClass) simpleRulesRuntimeEnv.getTopClass()).getCloner());
        }
        throw new IllegalStateException("SimpleRulesRuntimeEnv is expected");
    }

    @Override
    public IMethodCaller handle(IMethodCaller methodCaller,
                                JavaOpenMethod javaOpenMethod,
                                IOpenClass[] callParams,
                                ICastFactory castFactory) {
        return new AutoCastableResultOpenMethod(new MethodDetailsMethodCaller(methodCaller, this::buildMethodDetails),
                methodCaller.getMethod().getType(),
                JavaNoCast.getInstance());
    }

}
