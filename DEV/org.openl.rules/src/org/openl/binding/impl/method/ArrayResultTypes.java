package org.openl.binding.impl.method;

import java.util.ArrayList;
import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import org.openl.binding.ICastFactory;
import org.openl.rules.annotations.ArrayResultType;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.OpenClassUtils;

/**
 * Applies {@link ArrayResultType} to the caller of a rules function: the caller reports the array type that the
 * arguments give and converts the result of the function to it.
 * <p>
 * A generic function gets a primitive array as an array of its boxed type, so its result is boxed. The conversion
 * turns an {@code Integer[]} result back into the {@code int[]} that the call passed.
 *
 * @author Yury Molchan
 */
final class ArrayResultTypes {

    private ArrayResultTypes() {
    }

    /**
     * Returns the caller with the result type that the arguments give, or the caller itself when it already has this
     * type or the type is unknown.
     */
    static IMethodCaller wrap(ArrayResultType.Kind kind,
                              IMethodCaller methodCaller,
                              IOpenMethod method,
                              IOpenClass[] callParams,
                              boolean vararg,
                              ICastFactory castFactory) {
        var type = getResultType(kind, method, callParams, vararg, castFactory);
        var current = methodCaller.getMethod().getType();
        if (type == null || type.equals(current)) {
            return methodCaller;
        }
        var cast = castFactory.getCast(current, type);
        return cast == null ? methodCaller : new AutoCastableResultOpenMethod(methodCaller, type, cast);
    }

    private static @Nullable IOpenClass getResultType(ArrayResultType.Kind kind,
                                                      IOpenMethod method,
                                                      IOpenClass[] callParams,
                                                      boolean vararg,
                                                      ICastFactory castFactory) {
        var array = getArrayType(method, callParams, vararg, castFactory);
        if (array == null || kind == ArrayResultType.Kind.SAME) {
            return array;
        }
        var classes = new ArrayList<IOpenClass>();
        classes.add(array.getComponentClass());
        var last = method.getSignature().getNumberOfParameters() - 1;
        if (vararg) {
            classes.addAll(Arrays.asList(callParams).subList(last, callParams.length));
        } else if (method.getSignature().getParameterType(last).isArray() && callParams[last].isArray()) {
            classes.add(callParams[last].getComponentClass());
        } else {
            classes.add(callParams[last]);
        }
        var element = OpenClassUtils.findClosestElementClass(castFactory, classes);
        return element == null ? null : element.getArrayType(1);
    }

    /**
     * Returns the type of the array the first parameter takes: the type of the argument, or an array of the closest
     * type of the separate values passed to a vararg first parameter. Returns {@code null} for a missing array of an
     * unknown type.
     */
    private static @Nullable IOpenClass getArrayType(IOpenMethod method,
                                                     IOpenClass[] callParams,
                                                     boolean vararg,
                                                     ICastFactory castFactory) {
        if (callParams.length == 0) {
            return null;
        }
        if (vararg && method.getSignature().getNumberOfParameters() == 1) {
            var element = OpenClassUtils.findClosestElementClass(castFactory, Arrays.asList(callParams));
            return element == null ? null : element.getArrayType(1);
        }
        return callParams[0].isArray() ? callParams[0] : null;
    }
}
