package org.openl.binding.impl.method;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.openl.binding.ICastFactory;
import org.openl.binding.IMethodFactory;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.binding.impl.cast.CastsLinkageCast;
import org.openl.binding.impl.cast.IArrayOneElementCast;
import org.openl.binding.impl.cast.IOneElementArrayCast;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.MethodCallerWrapper;
import org.openl.binding.impl.cast.MethodCallerWrapperFactory;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.CastingMethodCaller;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenMethod;
import org.openl.util.ClassUtils;
import org.openl.util.CollectionUtils;
import org.openl.util.JavaGenericsUtils;

/**
 * @author snshor, Marat Kamalov
 *
 */
public final class MethodSearch {

    private MethodSearch() {
    }

    private static final Match NO_MATCH = new Match(null, null, null, null, null, null, null, null, false, null);

    private static Match calcMatch(IOpenMethod method,
            IOpenClass[] originalCallParams,
            IOpenClass[] callParam,
            ICastFactory castFactory,
            boolean vararg,
            IOpenClass varargElementType,
            boolean allowMultiCallParams) {
        final IOpenClass[] methodParam = method.getSignature().getParameterTypes();
        JavaOpenMethod javaOpenMethod = null;
        if (method instanceof JavaOpenMethod) {
            javaOpenMethod = (JavaOpenMethod) method;
        }

        int size = vararg ? originalCallParams.length + 1 : originalCallParams.length;

        final boolean[] multiCallParams = allowMultiCallParams ? new boolean[size] : null;
        final IOpenCast[] paramCasts = new IOpenCast[size];
        IOpenCast returnCast = null;
        IOpenClass returnType = null;
        Integer[] castDistances = new Integer[size];
        if (javaOpenMethod != null) {
            Map<String, IOpenClass> genericTypes = new HashMap<>();
            int countOfParameters = javaOpenMethod.getParameterTypes().length;
            String[] typeNames = new String[countOfParameters];
            int[] arrayDims = new int[countOfParameters];
            int i = 0;
            for (Type type : javaOpenMethod.getJavaMethod().getGenericParameterTypes()) {
                typeNames[i] = JavaGenericsUtils.getGenericTypeName(type);
                if (typeNames[i] != null) {
                    IOpenClass t = callParam[i];
                    if (NullOpenClass.isAnyNull(t)) {
                        continue;
                    }
                    arrayDims[i] = JavaGenericsUtils.getGenericTypeDim(type);
                    int arrayDim = arrayDims[i];
                    while (t.isArray() && arrayDim > 0) {
                        arrayDim--;
                        t = t.getComponentClass();
                    }
                    if (arrayDim > 0) {
                        if (arrayDim == 1 && allowMultiCallParams && callParam[i]
                            .isArray() && (!vararg || i != callParam.length - 1)) {
                            multiCallParams[i] = true;
                        } else {
                            return NO_MATCH;
                        }
                    }
                    t = unwrapPrimitiveClassIfNeeded(t);
                    if (genericTypes.containsKey(typeNames[i])) {
                        IOpenClass existedType = genericTypes.get(typeNames[i]);
                        IOpenClass clazz = castFactory.findClosestClass(t, existedType);
                        genericTypes.put(typeNames[i], unwrapPrimitiveClassIfNeeded(clazz));
                    } else {
                        genericTypes.put(typeNames[i], t);
                    }
                }
                i++;
            }

            for (i = 0; i < callParam.length; i++) {
                if (typeNames[i] != null && genericTypes.containsKey(typeNames[i])) {
                    IOpenClass type = genericTypes.get(typeNames[i]);
                    type = arrayDims[i] > 0 ? type.getArrayType(arrayDims[i]) : type;

                    IOpenClass cp = callParam[i];
                    if (allowMultiCallParams && multiCallParams[i]) {
                        cp = cp.getComponentClass();
                    }

                    IOpenCast gCast = castFactory.getCast(cp, type);
                    if (gCast == null || !gCast
                        .isImplicit() || allowMultiCallParams && gCast instanceof IArrayOneElementCast) {
                        if (allowMultiCallParams && cp
                            .isArray() && !multiCallParams[i] && (!vararg || i != callParam.length - 1)) {
                            cp = cp.getComponentClass();
                            gCast = castFactory.getCast(cp, type);
                            if (gCast == null || !gCast.isImplicit() || gCast instanceof IArrayOneElementCast) {
                                return NO_MATCH;
                            }
                            multiCallParams[i] = true;
                        } else {
                            return NO_MATCH;
                        }
                    }
                    if (!NullOpenClass.isAnyNull(cp) && !Objects.equals(cp, type)) {
                        if (!Objects.equals(type, methodParam[i])) {
                            IOpenCast cast = castFactory.getCast(type, methodParam[i]);
                            if (cast == null || !cast.isImplicit()) {
                                return NO_MATCH;
                            }
                            paramCasts[i] = new CastsLinkageCast(gCast, cast);
                        } else {
                            paramCasts[i] = gCast;
                        }
                        castDistances[i] = paramCasts[i].getDistance();
                    } else {
                        if (!Objects.equals(cp, methodParam[i])) {
                            paramCasts[i] = castFactory.getCast(cp, methodParam[i]);
                            if (paramCasts[i] == null || !paramCasts[i]
                                .isImplicit() || allowMultiCallParams && paramCasts[i] instanceof IArrayOneElementCast) {
                                if (allowMultiCallParams && cp
                                    .isArray() && !multiCallParams[i] && (!vararg || i != callParam.length - 1)) {
                                    cp = cp.getComponentClass();
                                    paramCasts[i] = castFactory.getCast(cp, methodParam[i]);
                                    if (paramCasts[i] == null || !paramCasts[i]
                                        .isImplicit() || paramCasts[i] instanceof IArrayOneElementCast) {
                                        return NO_MATCH;
                                    }
                                    multiCallParams[i] = true;
                                } else {
                                    return NO_MATCH;
                                }
                            }
                        }
                    }
                } else {
                    IOpenClass cp = callParam[i];
                    if (!Objects.equals(cp, methodParam[i])) {
                        if (IOpenClass.class.isAssignableFrom(methodParam[i].getInstanceClass()) && methodParam[i]
                            .getInstanceClass()
                            .isAssignableFrom(cp.getClass())) {
                            paramCasts[i] = castFactory.getCast(cp, JavaOpenClass.OBJECT);
                        } else {
                            paramCasts[i] = castFactory.getCast(cp, methodParam[i]);
                        }
                        if (paramCasts[i] == null || !paramCasts[i]
                            .isImplicit() || allowMultiCallParams && paramCasts[i] instanceof IArrayOneElementCast) {
                            if (allowMultiCallParams && cp
                                .isArray() && !multiCallParams[i] && (!vararg || i != callParam.length - 1)) {
                                cp = cp.getComponentClass();
                                if (IOpenClass.class.isAssignableFrom(
                                    methodParam[i].getInstanceClass()) && methodParam[i].getInstanceClass()
                                        .isAssignableFrom(cp.getClass())) {
                                    paramCasts[i] = castFactory.getCast(cp, JavaOpenClass.OBJECT);
                                } else {
                                    paramCasts[i] = castFactory.getCast(cp, methodParam[i]);
                                }
                                if (paramCasts[i] == null || !paramCasts[i]
                                    .isImplicit() || paramCasts[i] instanceof IArrayOneElementCast) {
                                    return NO_MATCH;
                                }
                                multiCallParams[i] = true;
                            } else {
                                return NO_MATCH;
                            }
                        }
                    }
                }
            }
            String returnTypeName = JavaGenericsUtils
                .getGenericTypeName(javaOpenMethod.getJavaMethod().getGenericReturnType());

            if (returnTypeName != null && genericTypes.containsKey(returnTypeName)) {
                int dim = JavaGenericsUtils.getGenericTypeDim(javaOpenMethod.getJavaMethod().getGenericReturnType());
                IOpenClass type = genericTypes.get(returnTypeName);
                if (dim > 0) {
                    type = type.getArrayType(dim);
                }
                returnCast = castFactory.getCast(javaOpenMethod.getType(), type);
                if (returnCast == null) {
                    return NO_MATCH;
                }
                returnType = type;
            }
        } else {
            for (int i = 0; i < callParam.length; i++) {
                IOpenClass cp = callParam[i];
                if (cp != methodParam[i]) {
                    IOpenCast cast = castFactory.getCast(callParam[i], methodParam[i]);
                    if (cast == null || !cast
                        .isImplicit() || allowMultiCallParams && cast instanceof IArrayOneElementCast) {
                        if (allowMultiCallParams && cp.isArray() && !multiCallParams[i]) {
                            cp = cp.getComponentClass();
                            cast = castFactory.getCast(cp, methodParam[i]);
                            if (cast == null || !cast.isImplicit() || cast instanceof IArrayOneElementCast) {
                                return NO_MATCH;
                            }
                            multiCallParams[i] = true;
                        } else {
                            return NO_MATCH;
                        }
                    }
                    paramCasts[i] = cast;
                }
            }
        }

        if (vararg) {
            for (int i = callParam.length - 1; i < size - 1; i++) {
                if (varargElementType != originalCallParams[i]) {
                    IOpenCast cast = castFactory.getCast(originalCallParams[i], varargElementType);
                    if (cast == null || !cast.isImplicit()) {
                        return NO_MATCH;
                    }
                    paramCasts[i + 1] = cast;
                }
            }
        }

        int[] m = new int[size];
        Arrays.fill(m, CastFactory.NO_CAST_DISTANCE);

        for (int i = 0; i < size; i++) {
            if (paramCasts[i] != null) {
                if (castDistances[i] == null) {
                    m[i] = paramCasts[i].getDistance();
                } else {
                    m[i] = castDistances[i];
                }
            }
        }

        Arrays.sort(m);

        if (vararg && NullOpenClass.isAnyNull(
            varargElementType) && originalCallParams.length >= method.getSignature().getNumberOfParameters()) {
            int lastParameterIndex = method.getSignature().getNumberOfParameters() - 1;
            varargElementType = method.getSignature().getParameterType(lastParameterIndex).getComponentClass();
        }

        return new Match(method,
            originalCallParams,
            callParam,
            paramCasts,
            multiCallParams,
            returnCast,
            returnType,
            m,
            vararg,
            varargElementType);

    }

    private static IOpenClass unwrapPrimitiveClassIfNeeded(IOpenClass clazz) {
        if (clazz != null && clazz.getInstanceClass() != null && clazz.getInstanceClass().isPrimitive()) {
            return JavaOpenClass.getOpenClass(ClassUtils.primitiveToWrapper(clazz.getInstanceClass()));
        }
        return clazz;
    }

    private static boolean isNoCastDistances(int[] m) {
        for (int value : m) {
            if (value != CastFactory.NO_CAST_DISTANCE) {
                return false;
            }
        }
        return true;
    }

    private static int getTypeDim(IOpenClass openClass) {
        int dim = 0;
        while (openClass.isArray()) {
            openClass = openClass.getComponentClass();
            dim++;
        }
        return dim;
    }

    private static int countMultiCallParams(Match match) {
        if (match == NO_MATCH || match.multiCallParams == null) {
            return Integer.MAX_VALUE;
        }
        return countTrues(match.multiCallParams);
    }

    private static int countTrues(boolean[] x) {
        if (x == null) {
            return 0;
        }
        int count = 0;
        for (boolean b : x) {
            if (b) {
                count++;
            }
        }
        return count;
    }

    private static boolean lq(Match match, Match bestMethodMatch) {
        if (bestMethodMatch == NO_MATCH) {
            return true;
        }
        if (match == NO_MATCH) {
            return false;
        }
        int[] dims1 = match.getSortedDims();
        int[] dims2 = bestMethodMatch.getSortedDims();
        int x = Math.max(dims1.length, dims2.length);
        for (int i = 0; i < x; i++) {
            int p1 = i < dims1.length ? dims1[dims1.length - 1 - i] : 0;
            int p2 = i < dims2.length ? dims2[dims2.length - 1 - i] : 0;

            if (p1 != p2) {
                return p1 < p2;
            }
        }

        // FIXME REMOVE IT
        if (match.isVararg() && !bestMethodMatch.isVararg()) {
            return false;
        }
        if (!bestMethodMatch.isVararg() && match.isVararg()) {
            return true;
        }
        // END FIXME

        int[] d1 = match.getSortedDistances();
        int[] d2 = bestMethodMatch.getSortedDistances();
        x = Math.max(d1.length, d2.length);
        for (int i = 0; i < x; i++) {
            int p1 = i < d1.length ? d1[d1.length - 1 - i] : CastFactory.NO_CAST_DISTANCE;
            int p2 = i < d2.length ? d2[d2.length - 1 - i] : CastFactory.NO_CAST_DISTANCE;

            if (p1 < p2) {
                return true;
            }
            if (p1 > p2) {
                return false;
            }
        }
        return false;
    }

    private static boolean eq(Match match1, Match match2) {
        int[] dims1 = match1.getSortedDims();
        int[] dims2 = match2.getSortedDims();
        int x = Math.max(dims1.length, dims2.length);
        for (int i = 0; i < x; i++) {
            int p1 = i < dims1.length ? dims1[dims1.length - 1 - i] : 0;
            int p2 = i < dims2.length ? dims2[dims2.length - 1 - i] : 0;
            if (p1 != p2) {
                return false;
            }
        }
        int[] d1 = match1.getSortedDistances();
        int[] d2 = match2.getSortedDistances();
        x = Math.max(d1.length, d2.length);
        for (int i = 0; i < x; i++) {
            int p1 = i < d1.length ? d1[d1.length - 1 - i] : CastFactory.NO_CAST_DISTANCE;
            int p2 = i < d2.length ? d2[d2.length - 1 - i] : CastFactory.NO_CAST_DISTANCE;
            if (p1 != p2) {
                return false;
            }
        }
        return true;
    }

    private static class Match {
        private final IOpenMethod method;
        private final IOpenClass[] callParams;
        private final IOpenClass[] originalCallParams;
        private final IOpenCast[] paramCasts;
        private final boolean[] multiCallParams;
        private final IOpenClass varargElementType;
        private final IOpenCast returnCast;
        private final IOpenClass returnType;
        private final int[] sortedDistances;
        private int[] sortedDims;
        private final boolean vararg;

        private IOpenClass[] mostSpecificParamsToCompare;

        private Match(IOpenMethod method,
                IOpenClass[] originalCallParams,
                IOpenClass[] callParams,
                IOpenCast[] paramCasts,
                boolean[] multiCallParams,
                IOpenCast returnCast,
                IOpenClass returnType,
                int[] distances,
                boolean vararg,
                IOpenClass varargElementType) {
            this.method = method;
            this.originalCallParams = originalCallParams;
            this.paramCasts = paramCasts;
            this.callParams = callParams;
            this.multiCallParams = multiCallParams;
            this.returnCast = returnCast;
            this.returnType = returnType;
            this.sortedDistances = distances;
            this.vararg = vararg;
            this.varargElementType = varargElementType;
        }

        public IOpenClass[] getVariableArityParameters() {
            if (mostSpecificParamsToCompare == null) {
                int size = originalCallParams.length;
                if (vararg && getMethod().getSignature().getNumberOfParameters() > originalCallParams.length) {
                    size++;
                }
                IOpenClass[] ret = new IOpenClass[size];
                if (ret.length > 0) {
                    int i = 0;
                    IOpenClass lastParameter = method.getSignature()
                        .getParameterType(method.getSignature().getNumberOfParameters() - 1);
                    while (i < ret.length) {
                        if (i < method.getSignature().getNumberOfParameters() - (vararg ? 1 : 0)) {
                            ret[i] = method.getSignature().getParameterType(i);
                        } else if (vararg) {
                            if (getMethod().getSignature().getNumberOfParameters() > originalCallParams.length) {
                                ret[i] = NullOpenClass.the;
                            } else {
                                ret[i] = lastParameter.getComponentClass();
                            }
                        } else {
                            ret[i] = lastParameter;
                        }
                        i++;
                    }
                }
                mostSpecificParamsToCompare = ret;
            }
            return mostSpecificParamsToCompare;
        }

        private boolean isMoreSpecific(Match other, ICastFactory casts) {
            IOpenClass[] firstParams = this.getVariableArityParameters();
            IOpenClass[] secondParams = other.getVariableArityParameters();
            int x = Math.min(firstParams.length, secondParams.length);
            boolean differenceInArgTypes = false;
            // more specific arg types
            for (int i = 0; i < x; i++) {
                IOpenClass firstArgType = firstParams[i];
                IOpenClass secondArgType = secondParams[i];
                if (!firstArgType.equals(secondArgType)) {
                    differenceInArgTypes = true;
                    IOpenCast cast = casts.getCast(firstArgType, secondArgType);
                    if (cast == null || !cast.isImplicit()) {
                        return false;
                    }
                }
            }
            if (!differenceInArgTypes) {
                if (this.isVararg() && !other.isVararg()) {
                    return false;
                }
                if (!this.isVararg() && other.isVararg()) {
                    return true;
                }
                // more specific declaring class
                IOpenClass firstDeclaringClass = this.getMethod().getDeclaringClass();
                IOpenClass secondDeclaringClass = other.getMethod().getDeclaringClass();
                return !firstDeclaringClass.equals(secondDeclaringClass) && secondDeclaringClass
                    .isAssignableFrom(firstDeclaringClass);
            } else {
                return true;
            }
        }

        public IOpenClass[] getOriginalCallParams() {
            return originalCallParams;
        }

        public IOpenClass getVarargElementType() {
            return varargElementType;
        }

        public boolean isVararg() {
            return vararg;
        }

        public IOpenMethod getMethod() {
            return method;
        }

        public int[] getSortedDistances() {
            return sortedDistances;
        }

        public boolean[] getMultiCallParams() {
            return multiCallParams;
        }

        public IOpenCast getReturnCast() {
            return returnCast;
        }

        public IOpenClass getReturnType() {
            return returnType;
        }

        public IOpenCast[] getParamCasts() {
            return paramCasts;
        }

        public int[] getSortedDims() {
            if (sortedDims == null) {
                IOpenClass[] variableArityParameters = getVariableArityParameters();
                int[] dims = new int[variableArityParameters.length];
                for (int i = 0; i < variableArityParameters.length; i++) {
                    if (i < originalCallParams.length) {
                        if (!NullOpenClass.isAnyNull(originalCallParams[i])) {
                            int cpDim = getTypeDim(originalCallParams[i]);
                            IOpenClass openClass = variableArityParameters[i];
                            int dim = 0;
                            while (openClass.isArray()) {
                                openClass = openClass.getComponentClass();
                                dim++;
                            }
                            dims[i] = Math.abs(dim - cpDim);
                        }
                        // FIXME REMOVE IT
                        if (vararg && i >= callParams.length - 1) {
                            dims[i]++;
                        }
                        // END FIXME
                    }
                }
                Arrays.sort(dims);
                sortedDims = dims;
            }
            return sortedDims;
        }

        public IOpenClass[] getCallParams() {
            return callParams;
        }
    }

    private static IMethodCaller findCastingMethod(final String name,
            IOpenClass[] callParams,
            ICastFactory castFactory,
            Iterable<IOpenMethod> methods,
            boolean allowMultiCallParams) throws AmbiguousMethodException {
        final int nParams = callParams.length;
        Iterable<IOpenMethod> filtered = methods == null ? Collections.emptyList()
                                                         : CollectionUtils.findAll(methods,
                                                             method -> method.getName().equals(name) && (method
                                                                 .getSignature()
                                                                 .getNumberOfParameters() == nParams || method
                                                                     .getSignature()
                                                                     .getNumberOfParameters() > 0 && method
                                                                         .getSignature()
                                                                         .getNumberOfParameters() <= callParams.length + 1 && method
                                                                             .getSignature()
                                                                             .getParameterType(method.getSignature()
                                                                                 .getNumberOfParameters() - 1)
                                                                             .isArray()));
        if (!filtered.iterator().hasNext()) {
            return null;
        }
        List<Match> matchingResult = new ArrayList<>();
        Match bestMethodMatch = NO_MATCH;
        long bestOneElementToArrayCastCount = Integer.MAX_VALUE;
        int bestMultiCallParamsCount = allowMultiCallParams ? Integer.MAX_VALUE : 0;
        LazyVarargTypeCalculator lazyVarargTypeCalculatorParentClass = new LazyVarargTypeCalculator(callParams,
            castFactory::findParentClass);
        LazyVarargTypeCalculator lazyVarargTypeCalculatorClosestClass = new LazyVarargTypeCalculator(callParams,
            castFactory::findClosestClass);
        for (IOpenMethod method : filtered) {
            List<Match> matches = new ArrayList<>();
            if (method.getSignature().getNumberOfParameters() == callParams.length) {
                Match noVarargMatch = calcMatch(method,
                    callParams,
                    callParams,
                    castFactory,
                    false,
                    null,
                    allowMultiCallParams);
                matches.add(noVarargMatch);
            }
            if (method.getSignature()
                .getNumberOfParameters() > 0 && method.getSignature()
                    .getParameterTypes()[method.getSignature().getNumberOfParameters() - 1].isArray() && allowVarargs(
                        method)) {
                IOpenClass varargElementTypeByParentClass = lazyVarargTypeCalculatorParentClass
                    .getElementType(method.getSignature().getNumberOfParameters() - 1);
                if (varargElementTypeByParentClass != null) {
                    matches.add(calcMatch(method,
                        callParams,
                        lazyVarargTypeCalculatorParentClass
                            .getVarargMethodCallParams(method.getSignature().getNumberOfParameters() - 1),
                        castFactory,
                        true,
                        varargElementTypeByParentClass,
                        allowMultiCallParams));
                }

                IOpenClass varargElementTypeByClosestClass = lazyVarargTypeCalculatorClosestClass
                    .getElementType(method.getSignature().getNumberOfParameters() - 1);
                if (varargElementTypeByClosestClass != null && varargElementTypeByClosestClass != varargElementTypeByParentClass) {
                    matches.add(calcMatch(method,
                        callParams,
                        lazyVarargTypeCalculatorClosestClass
                            .getVarargMethodCallParams(method.getSignature().getNumberOfParameters() - 1),
                        castFactory,
                        true,
                        varargElementTypeByClosestClass,
                        allowMultiCallParams));
                }
            }
            boolean f = false;
            for (Match match : matches) {
                if (match == NO_MATCH) {
                    continue;
                }
                long oneElementToArrayCastCount = Arrays.stream(match.paramCasts)
                    .filter(e -> e instanceof IOneElementArrayCast)
                    .count();
                int multiCallParamsHolderCount = allowMultiCallParams ? countMultiCallParams(match) : 0;
                if (oneElementToArrayCastCount < bestOneElementToArrayCastCount || oneElementToArrayCastCount == bestOneElementToArrayCastCount && multiCallParamsHolderCount < bestMultiCallParamsCount || oneElementToArrayCastCount == bestOneElementToArrayCastCount && multiCallParamsHolderCount == bestMultiCallParamsCount && lq(
                    match,
                    bestMethodMatch)) {
                    bestMethodMatch = match;
                    bestMultiCallParamsCount = multiCallParamsHolderCount;
                    bestOneElementToArrayCastCount = oneElementToArrayCastCount;
                    matchingResult.clear();
                    matchingResult.add(match);
                    f = true;
                    continue;
                }

                if (oneElementToArrayCastCount == bestOneElementToArrayCastCount && multiCallParamsHolderCount == bestMultiCallParamsCount && eq(
                    match,
                    bestMethodMatch)) {
                    if (!f) {
                        matchingResult.add(match);
                        f = true;
                    }
                }
            }
        }

        IMethodCaller methodCaller = null;
        Match selectedMatch = null;
        switch (matchingResult.size()) {
            case 0:
                break;
            case 1:
                selectedMatch = matchingResult.get(0);
                IOpenMethod m = selectedMatch.getMethod();
                if (!isNoCastDistances(selectedMatch.getSortedDistances())) {
                    IOpenCast[] paramCasts = getParamCastsAndTruncateIfNeed(selectedMatch);
                    CastingMethodCaller methodCaller1 = new CastingMethodCaller(m, paramCasts);
                    methodCaller = buildMethod(selectedMatch.getReturnCast(),
                        selectedMatch.getReturnType(),
                        m,
                        methodCaller1);
                } else {
                    methodCaller = buildMethod(selectedMatch.getReturnCast(), selectedMatch.getReturnType(), m, m);
                }
                break;
            default:
                int mostSpecificMatchIndex = findMostSpecific(name, callParams, matchingResult, castFactory);
                selectedMatch = matchingResult.get(mostSpecificMatchIndex);
                IOpenMethod method = selectedMatch.getMethod();
                boolean f = true;
                for (int i = 0; i < nParams; i++) {
                    if (!callParams[i].equals(method.getSignature().getParameterType(i))) {
                        f = false;
                        break;
                    }
                }
                if (f) {
                    methodCaller = method;
                } else {
                    IOpenCast[] paramCasts = getParamCastsAndTruncateIfNeed(selectedMatch);
                    CastingMethodCaller methodCaller1 = new CastingMethodCaller(method, paramCasts);
                    if (selectedMatch.getReturnCast() != null && selectedMatch.getReturnType() != method.getType()) {
                        methodCaller = new AutoCastableResultOpenMethod(methodCaller1,
                            selectedMatch.getReturnType(),
                            selectedMatch.getReturnCast());
                    } else {
                        methodCaller = methodCaller1;
                    }
                }
        }
        if (methodCaller != null) {
            if (selectedMatch.isVararg()) {
                IOpenCast[] paramCasts = new IOpenCast[callParams.length - selectedMatch.getCallParams().length + 1];
                System.arraycopy(selectedMatch.getParamCasts(),
                    selectedMatch.getCallParams().length,
                    paramCasts,
                    0,
                    callParams.length - selectedMatch.getCallParams().length + 1);
                if (selectedMatch.getCallParams().length <= callParams.length) {
                    methodCaller = new VarArgsOpenMethod(methodCaller,
                        selectedMatch.getVarargElementType().getInstanceClass(),
                        selectedMatch.getCallParams().length - 1,
                        paramCasts);
                } else {
                    methodCaller = new NullVarArgsOpenMethod(methodCaller);
                }
            }

            if (selectedMatch.getMethod() instanceof JavaOpenMethod) {
                methodCaller = processJavaAnnotationsOnMethod(callParams, castFactory, methodCaller, selectedMatch);
            }

            if (selectedMatch.getMultiCallParams() != null && countTrues(selectedMatch.getMultiCallParams()) > 0) {
                return new MultiCallOpenMethod(methodCaller, selectedMatch.getMultiCallParams());
            } else {
                return methodCaller;
            }
        }
        return null;
    }

    private static IMethodCaller processJavaAnnotationsOnMethod(IOpenClass[] callParams,
            ICastFactory castFactory,
            IMethodCaller methodCaller,
            Match selectedMatch) {
        JavaOpenMethod javaOpenMethod = (JavaOpenMethod) selectedMatch.getMethod();
        Method javaMethod = javaOpenMethod.getJavaMethod();
        MethodCallerWrapper methodCallerWrapper = javaMethod.getAnnotation(MethodCallerWrapper.class);
        if (methodCallerWrapper != null) {
            Class<? extends MethodCallerWrapperFactory> clazz = methodCallerWrapper.value();
            try {
                MethodCallerWrapperFactory methodCallerWrapperFactory = clazz.newInstance();
                methodCaller = methodCallerWrapperFactory.build(castFactory, methodCaller, javaOpenMethod, callParams);
            } catch (InstantiationException | IllegalAccessException ignored) {
            }
        }
        return methodCaller;
    }

    private static IOpenCast[] getParamCastsAndTruncateIfNeed(Match selectedMatch) {
        IOpenCast[] paramCasts = selectedMatch.getParamCasts();
        if (paramCasts.length != selectedMatch.getCallParams().length) {
            paramCasts = new IOpenCast[selectedMatch.getCallParams().length];
            System.arraycopy(selectedMatch.getParamCasts(), 0, paramCasts, 0, selectedMatch.getCallParams().length);
        }
        return paramCasts;
    }

    private static boolean allowVarargs(IOpenMethod method) {
        if (method instanceof JavaOpenMethod) {
            JavaOpenMethod javaOpenMethod = (JavaOpenMethod) method;
            if (javaOpenMethod.getJavaMethod().isAnnotationPresent(IgnoreVarargsMatching.class)) {
                return false;
            }
            return !javaOpenMethod.getJavaMethod().getDeclaringClass().isAnnotationPresent(IgnoreVarargsMatching.class);
        }
        return true;
    }

    private static class LazyVarargTypeCalculator {
        private final IOpenClass[] callParams;
        private final BiFunction<IOpenClass, IOpenClass, IOpenClass> func;
        private final IOpenClass[] varArgElementTypes;
        private int lastCalculated;
        private IOpenClass lastVarArgElementType;
        private final IOpenClass[][] varargMethodCallParamsCache;

        public LazyVarargTypeCalculator(IOpenClass[] callParams, BiFunction<IOpenClass, IOpenClass, IOpenClass> func) {
            this.callParams = callParams;
            this.varArgElementTypes = new IOpenClass[callParams.length + 1];
            this.varArgElementTypes[varArgElementTypes.length - 1] = NullOpenClass.the;
            this.lastCalculated = callParams.length;
            this.varargMethodCallParamsCache = new IOpenClass[callParams.length + 1][];
            this.func = func;
        }

        private IOpenClass getElementType(int index) {
            if (lastCalculated > index) {
                for (int i = lastCalculated - 1; i >= 0; i--) {
                    this.lastVarArgElementType = i == callParams.length - 1 ? callParams[i]
                                                                            : func.apply(callParams[i],
                                                                                lastVarArgElementType);
                    if (lastVarArgElementType == null) {
                        lastCalculated = 0;
                        return null;
                    } else {
                        varArgElementTypes[i] = this.lastVarArgElementType;
                    }
                }
            }
            return varArgElementTypes[index];
        }

        public IOpenClass[] getVarargMethodCallParams(int index) {
            if (varargMethodCallParamsCache[index] == null) {
                IOpenClass[] varargMethodCallParams = new IOpenClass[index + 1];
                System.arraycopy(this.callParams, 0, varargMethodCallParams, 0, index);
                IOpenClass varargElementType = getElementType(index);
                if (varargElementType == null || NullOpenClass.isAnyNull(varargElementType)) {
                    varargMethodCallParams[index] = varargElementType;
                } else {
                    varargMethodCallParams[index] = varargElementType.getAggregateInfo()
                        .getIndexedAggregateType(varargElementType);
                }
                this.varargMethodCallParamsCache[index] = varargMethodCallParams;
                return varargMethodCallParams;
            } else {
                return varargMethodCallParamsCache[index];
            }
        }
    }

    private static IMethodCaller buildMethod(IOpenCast methodsReturnCast,
            IOpenClass methodsReturnType,
            IOpenMethod m,
            IMethodCaller methodCaller) {
        if (methodsReturnCast != null && !methodsReturnType.equals(m.getType())) {
            return new AutoCastableResultOpenMethod(methodCaller, methodsReturnType, methodsReturnCast);
        } else {
            return methodCaller;
        }
    }

    /**
     * Choosing the most specific method according to:
     *
     * @see <a href= "http://java.sun.com/docs/books/jls/second_edition/html/expressions.doc.html#18428" >java
     *      documentation </a >
     *
     *
     * @param name The name of the method.
     * @param params Argument types of the method.
     * @param matches All matching methods for this argument types.
     * @param casts OpenL cast factory.
     *
     * @return The most specific method from matching methods collection.
     *
     * @throws AmbiguousMethodException Exception will be thrown if most specific method cannot be determined.
     */
    private static int findMostSpecific(String name,
            IOpenClass[] params,
            List<Match> matches,
            ICastFactory casts) throws AmbiguousMethodException {
        List<Integer> moreSpecificIndexes = new ArrayList<>();
        for (int i = 0; i < matches.size(); i++) {
            Match res = matches.get(i);
            boolean f = true;
            for (Match next : matches) {
                if (res != next && !res.isMoreSpecific(next, casts)) {
                    f = false;
                    break;
                }
            }
            if (f) {
                moreSpecificIndexes.add(i);
            }
        }

        if (moreSpecificIndexes.size() == 1) {
            return moreSpecificIndexes.get(0);
        } else {
            List<Integer> mostSpecificIndexes = new ArrayList<>();
            int best1 = Integer.MAX_VALUE;
            int best2 = Integer.MAX_VALUE;
            for (Integer index : moreSpecificIndexes) {
                IOpenMethod m = matches.get(index).getMethod();
                int penalty1 = 0;
                int penalty2 = 0;
                if (m.getSignature().getNumberOfParameters() == params.length) {
                    for (int i = 0; i < params.length; i++) {
                        if ((NullOpenClass.isAnyNull(params[i]) || !params[i].getInstanceClass().isPrimitive()) && m
                            .getSignature()
                            .getParameterType(i)
                            .getInstanceClass()
                            .isPrimitive()) {
                            penalty1++;
                        }
                        if ((!NullOpenClass.isAnyNull(params[i]) && params[i].getInstanceClass().isPrimitive()) != m
                            .getSignature()
                            .getParameterType(i)
                            .getInstanceClass()
                            .isPrimitive() || (NullOpenClass.isAnyNull(
                                params[i]) && !m.getSignature().getParameterType(i).getInstanceClass().isPrimitive())) {
                            penalty2++;
                        }
                    }
                }
                if (penalty1 < best1) {
                    best1 = penalty1;
                    best2 = penalty2;
                    mostSpecificIndexes.clear();
                    mostSpecificIndexes.add(index);
                } else {
                    if (penalty1 == best1) {
                        if (penalty2 < best2) {
                            best2 = penalty2;
                            mostSpecificIndexes.clear();
                            mostSpecificIndexes.add(index);
                        } else {
                            if (penalty2 == best2) {
                                mostSpecificIndexes.add(index);
                            }
                        }
                    }
                }
            }

            int countOfFoundMethods = mostSpecificIndexes.size();
            if (countOfFoundMethods == 1) {
                return mostSpecificIndexes.get(0);
            } else if (countOfFoundMethods == 0) {
                throw new AmbiguousMethodException(name,
                    params,
                    matches.stream().map(Match::getMethod).collect(Collectors.toList()));
            } else {
                throw new AmbiguousMethodException(name,
                    params,
                    moreSpecificIndexes.stream().map(matches::get).map(Match::getMethod).collect(Collectors.toList()));
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IMethodFactory#getMethod(java.lang.String, org.openl.types.IOpenClass[],
     * org.openl.binding.ICastFactory)
     */
    public static IMethodCaller findMethod(String name,
            IOpenClass[] params,
            ICastFactory castFactory,
            IMethodFactory factory,
            boolean allowMultiCalls) throws AmbiguousMethodException {
        return findMethod(name, params, castFactory, factory, false, allowMultiCalls);
    }

    public static IMethodCaller findConstructor(IOpenClass[] params,
            ICastFactory casts,
            IMethodFactory factory) throws AmbiguousMethodException {
        IMethodCaller caller;
        if (factory instanceof ADynamicClass) {
            caller = ((ADynamicClass) factory).getConstructor(params, true);
        } else {
            caller = factory.getConstructor(params);
        }
        if (caller != null) {
            return caller;
        }
        if (params.length == 0 || casts == null) {
            return null;
        }
        return findCastingMethod("<init>", params, casts, factory.constructors(), false);
    }

    public static IMethodCaller findMethod(String name,
            IOpenClass[] params,
            ICastFactory castFactory,
            IMethodFactory factory,
            boolean strictMatch,
            boolean allowMultiCalls) throws AmbiguousMethodException {
        IMethodCaller caller;
        if (factory instanceof ADynamicClass) {
            ADynamicClass aDynamicClass = (ADynamicClass) factory;
            caller = aDynamicClass.getMethod(name, params, true);
        } else {
            caller = factory.getMethod(name, params);
        }
        if (caller != null) {
            return caller;
        }
        if (castFactory == null) {
            return null;
        }
        if (!strictMatch) {
            return findMethod(name, params, castFactory, factory.methods(name), allowMultiCalls);
        }
        return null;
    }

    public static IMethodCaller findMethod(String name,
            IOpenClass[] params,
            ICastFactory castFactory,
            Iterable<IOpenMethod> methods,
            boolean allowMultiCalls) throws AmbiguousMethodException {
        return findCastingMethod(name, params, castFactory, methods, allowMultiCalls);
    }
}
