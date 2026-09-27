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
        int[] dims = getDimensions(callParams);
        boolean[] paramAsElement = new boolean[callParams.length];
        var maxDim = 0;
        for (var i = 0; i < callParams.length; i++) {
            if (maxDim < dims[i]) {
                maxDim = dims[i];
            }
        }
        if (maxDim == 0) {
            maxDim = 1;
        }

        var minDim = getMinDimension(callParams, dims, maxDim);
        IOpenClass t = null;
        for (var i = 0; i < callParams.length; i++) {
            if (t == null && maxDim == dims[i]) {
                t = callParams[i];
            } else if (t != null && maxDim == dims[i]) {
                t = castFactory.findClosestClass(t, callParams[i]);
            }
            paramAsElement[i] = maxDim != dims[i];
        }
        if (t == null) {
            t = getDefaultType(callParams);
        }

        var dim = 0;
        var g = t;
        while (g.isArray()) {
            g = g.getComponentClass();
            dim++;
        }
        var type = g.getArrayType(dim);
        IOpenCast[] openCasts = getOpenCasts(callParams, paramAsElement, type, castFactory);
        return new AddAllMethodDetails(minDim, maxDim, type, paramAsElement, openCasts);
    }

    /**
     * Returns the array dimensions of each parameter. A parameter of an unknown or null type has none.
     */
    private static int[] getDimensions(IOpenClass[] callParams) {
        int[] dims = new int[callParams.length];
        for (var i = 0; i < callParams.length; i++) {
            if (!NullOpenClass.isAnyNull(callParams[i])) {
                var dim = 0;
                var t = callParams[i];
                while (t.isArray()) {
                    t = t.getComponentClass();
                    dim++;
                }
                dims[i] = dim;
            }
        }
        return dims;
    }

    /**
     * Returns the smallest dimension of the parameters. A parameter of an unknown or null type counts as one
     * dimension less than the largest one.
     */
    private static int getMinDimension(IOpenClass[] callParams, int[] dims, int maxDim) {
        var minDim = Integer.MAX_VALUE;
        for (var i = 0; i < callParams.length; i++) {
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
        return minDim;
    }

    /**
     * Returns the array type of the last parameter of a known type, or an array of objects when there is none.
     */
    private static IOpenClass getDefaultType(IOpenClass[] callParams) {
        IOpenClass t = null;
        for (IOpenClass callParam : callParams) {
            if (callParam != null && !NullOpenClass.isAnyNull(callParam)) {
                t = callParam.getArrayType(1);
            }
        }
        if (t == null) {
            t = JavaOpenClass.OBJECT.getArrayType(1);
        }
        return t;
    }

    private static IOpenCast[] getOpenCasts(IOpenClass[] callParams,
                                            boolean[] paramAsElement,
                                            IOpenClass type,
                                            ICastFactory castFactory) {
        IOpenCast[] openCasts = new IOpenCast[callParams.length];
        for (var i = 0; i < callParams.length; i++) {
            if (callParams[i] != null && !NullOpenClass.isAnyNull(callParams[i])) {
                openCasts[i] = castFactory.getCast(
                        paramAsElement[i] ? callParams[i] : callParams[i].getComponentClass(),
                        type.getComponentClass());
            }
        }
        return openCasts;
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
