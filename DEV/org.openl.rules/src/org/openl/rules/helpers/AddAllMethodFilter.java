package org.openl.rules.helpers;

import java.util.ArrayList;

import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.MethodFilter;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenMethod;
import org.openl.util.OpenClassUtils;

/**
 * Implementation of {@link MethodFilter} for addAll method from {@link RulesUtils}. The implementation controls that if
 * method parameters are arrays with difference in dimensions more than two, then the method must be not found.
 */
public class AddAllMethodFilter implements MethodFilter {

    /**
     * Works out the joined array for the arguments: its type and the casts of the arguments to its elements.
     * <p>
     * The arguments with the most dimensions are arrays to join, and the others are elements to add. The element type
     * of the result is the closest common type of the array elements and the added elements, see
     * {@link OpenClassUtils#findClosestElementClass}: an {@code int[]} with an {@code int} gives an {@code int[]}, with
     * an {@code Integer} or an empty value an {@code Integer[]}, and with a {@code double} a {@code double[]}.
     */
    public static AddAllMethodDetails resolve(IOpenClass[] callParams, ICastFactory castFactory) {
        int[] dims = getDimensions(callParams);
        var maxDim = 1;
        for (var dim : dims) {
            maxDim = Math.max(maxDim, dim);
        }
        var minDim = getMinDimension(callParams, dims, maxDim);
        boolean[] paramAsElement = new boolean[callParams.length];
        var elementClasses = new ArrayList<IOpenClass>();
        for (var i = 0; i < callParams.length; i++) {
            paramAsElement[i] = maxDim != dims[i];
            elementClasses.add(paramAsElement[i] ? callParams[i] : callParams[i].getComponentClass());
        }
        var element = OpenClassUtils.findClosestElementClass(castFactory, elementClasses);
        var type = (element == null ? JavaOpenClass.OBJECT : element).getArrayType(1);
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
