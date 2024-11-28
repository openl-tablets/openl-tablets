package org.openl.rules.helpers;

import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.JavaNoCast;
import org.openl.binding.impl.cast.MethodCallerWrapper;
import org.openl.binding.impl.cast.MethodDetailsMethodCaller;
import org.openl.binding.impl.method.AutoCastableResultOpenMethod;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenMethod;

/**
 * Implementation of {@link MethodCallerWrapper} for addAll method from {@link RulesUtils}.
 */
public class AddAllMethodCallerWrapper implements MethodCallerWrapper {

    @Override
    public IMethodCaller handle(IMethodCaller methodCaller,
                                JavaOpenMethod javaOpenMethod,
                                IOpenClass[] callParams,
                                ICastFactory castFactory) {
        final AddAllMethodDetails addAllMethodDetails = AddAllMethodFilter.resolve(callParams, castFactory);
        return new AutoCastableResultOpenMethod(new MethodDetailsMethodCaller(methodCaller, e -> addAllMethodDetails),
                addAllMethodDetails.getType(),
                JavaNoCast.getInstance());
    }
}
