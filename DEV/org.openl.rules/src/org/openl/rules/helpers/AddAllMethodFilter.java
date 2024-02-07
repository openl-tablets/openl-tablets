package org.openl.rules.helpers;

import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.MethodFilter;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenMethod;

/**
 * Implementation of {@link MethodFilter} for addAll method from {@link RulesUtils}. The implementation controls that if
 * method parameters are arrays with difference in dimensions more than two, then the method must be not found.
 */
public class AddAllMethodFilter implements MethodFilter {

    public static AddAllMethodDetails resolve(IOpenClass[] callParams, ICastFactory castFactory) {
        int[] dims = new int[callParams.length];
        boolean[] paramAsElement = new boolean[callParams.length];
        for (int i = 0; i < callParams.length; i++) {
            if (!NullOpenClass.isAnyNull(callParams[i])) {
                int dim = 0;
                IOpenClass t = callParams[i];
                while (t.isArray()) {
                    t = t.getComponentClass();
                    dim++;
                }
                dims[i] = dim;
            }
        }
        int maxDim = 0;
        int minDim = Integer.MAX_VALUE;
        for (int i = 0; i < callParams.length; i++) {
            if (maxDim < dims[i]) {
                maxDim = dims[i];
            }
        }
        if (maxDim == 0) {
            maxDim = 1;
        }

        for (int i = 0; i < callParams.length; i++) {
            if (!NullOpenClass.isAnyNull(callParams[i])) {
                if (dims[i] < minDim) {
                    minDim = dims[i];
                }
            } else {
                if (maxDim - 1 < minDim) {
                    minDim = maxDim - 1;
                }
            }
        }
        IOpenClass t = null;
        for (int i = 0; i < callParams.length; i++) {
            if (t == null && maxDim == dims[i]) {
                t = callParams[i];
            } else if (t != null && maxDim == dims[i]) {
                t = castFactory.findClosestClass(t, callParams[i]);
            }
            paramAsElement[i] = !(maxDim == dims[i]);
        }
        if (t == null) {
            for (IOpenClass callParam : callParams) {
                if (callParam != null && !NullOpenClass.isAnyNull(callParam)) {
                    t = callParam.getArrayType(1);
                }
            }
            if (t == null) {
                t = JavaOpenClass.OBJECT.getArrayType(1);
            }
        }

        int dim = 0;
        IOpenClass g = t;
        while (g.isArray()) {
            g = g.getComponentClass();
            dim++;
        }
        IOpenClass type = g.getArrayType(dim);
        IOpenCast[] openCasts = new IOpenCast[callParams.length];
        for (int i = 0; i < callParams.length; i++) {
            if (callParams[i] != null && !NullOpenClass.isAnyNull(callParams[i])) {
                openCasts[i] = castFactory.getCast(
                        paramAsElement[i] ? callParams[i] : callParams[i].getComponentClass(),
                        type.getComponentClass());
            }
        }
        return new AddAllMethodDetails(minDim, maxDim, type, paramAsElement, openCasts);
    }

    @Override
    public boolean predicate(JavaOpenMethod javaOpenMethod, IOpenClass[] callParams, ICastFactory castFactory) {
        if (callParams.length == 0) {
            return true;
        }
        final AddAllMethodDetails addAllMethodDetails = resolve(callParams, castFactory);
        return addAllMethodDetails.getMaxDim() - addAllMethodDetails.getMinDim() <= 1;
    }
}
