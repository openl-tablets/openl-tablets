package org.openl.rules.helpers;

import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.JavaNoCast;
import org.openl.binding.impl.cast.MethodCallerWrapper;
import org.openl.binding.impl.cast.MethodDetailsMethodCaller;
import org.openl.binding.impl.method.AutoCastableResultOpenMethod;
import org.openl.rules.table.OpenLCloner;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenMethod;

/**
 * Implementation of {@link MethodCallerWrapper} for copy method from {@link RulesUtils}.
 */
public class CopyMethodCallerWrapper implements MethodCallerWrapper {

    @Override
    public IMethodCaller handle(IMethodCaller methodCaller,
            JavaOpenMethod javaOpenMethod,
            IOpenClass[] callParams,
            ICastFactory castFactory) {
        return new AutoCastableResultOpenMethod(new MethodDetailsMethodCaller(methodCaller,
            new CopyMethodDetails(new OpenLCloner())), methodCaller.getMethod().getType(), JavaNoCast.getInstance());
    }

}
