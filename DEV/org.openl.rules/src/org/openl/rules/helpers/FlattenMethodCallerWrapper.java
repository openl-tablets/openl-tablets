package org.openl.rules.helpers;

import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.JavaNoCast;
import org.openl.binding.impl.cast.MethodCallerWrapper;
import org.openl.binding.impl.cast.MethodDetailsMethodCaller;
import org.openl.binding.impl.method.AutoCastableResultOpenMethod;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenMethod;

/**
 * Implementation of {@link MethodCallerWrapper} for flatten method from {@link RulesUtils}.
 */
public class FlattenMethodCallerWrapper implements MethodCallerWrapper {

    @Override
    public IMethodCaller handle(IMethodCaller methodCaller,
                                JavaOpenMethod javaOpenMethod,
                                IOpenClass[] callParams,
                                ICastFactory castFactory) {
        final int[] dims = new int[callParams.length];
        IOpenClass t = null;
        IOpenClass[] rootComponentClasses = new IOpenClass[callParams.length];
        for (int i = 0; i < callParams.length; i++) {
            IOpenClass g = callParams[i];
            int dim = 0;
            while (g.isArray()) {
                g = g.getComponentClass();
                dim++;
            }
            rootComponentClasses[i] = g;
            dims[i] = dim;
            if (t == null) {
                t = g;
            } else {
                t = castFactory.findClosestClass(t, g);
            }
        }
        if (t == null || NullOpenClass.isAnyNull(t)) {
            t = JavaOpenClass.OBJECT;
        }
        final IOpenCast[] openCasts = new IOpenCast[callParams.length];
        for (int i = 0; i < callParams.length; i++) {
            openCasts[i] = castFactory.getCast(rootComponentClasses[i], t);
        }
        final FlattenMethodDetails flattenMethodDetails = new FlattenMethodDetails(t.getArrayType(1), dims, openCasts);
        return new AutoCastableResultOpenMethod(new MethodDetailsMethodCaller(methodCaller, e -> flattenMethodDetails),
                t.getArrayType(1),
                JavaNoCast.getInstance());
    }

}
