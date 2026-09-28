package org.openl.binding.impl.method;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import lombok.Getter;

import org.openl.binding.ICastFactory;
import org.openl.binding.IMethodFactory;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.binding.impl.cast.CastsLinkageCast;
import org.openl.binding.impl.cast.IArrayOneElementCast;
import org.openl.binding.impl.cast.IOneElementArrayCast;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.MethodCallerWrapper;
import org.openl.binding.impl.cast.MethodFilter;
import org.openl.binding.impl.cast.MethodSearchTuner;
import org.openl.rules.annotations.IgnoreNonVarargsMatching;
import org.openl.rules.annotations.IgnoreVarargsMatching;
import org.openl.rules.annotations.NonNullLiteral;
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
        return new MatchCalculation(method, originalCallParams, callParam, castFactory, vararg, allowMultiCallParams)
                .calculate(varargElementType);
    }

    /**
     * Calculates how the call parameters match the parameters of a method: the casts of the parameters, the
     * parameters called with multiple values, and the cast of a generic return type.
     */
    private static final class MatchCalculation {
        private final IOpenMethod method;
        private final IOpenClass[] originalCallParams;
        private final IOpenClass[] callParam;
        private final ICastFactory castFactory;
        private final boolean vararg;
        private final boolean allowMultiCallParams;
        private final IOpenClass[] methodParam;
        private final int size;
        private final boolean[] multiCallParams;
        private final IOpenCast[] paramCasts;
        private final Integer[] castDistances;
        private IOpenCast returnCast;
        private IOpenClass returnType;

        private MatchCalculation(IOpenMethod method,
                                 IOpenClass[] originalCallParams,
                                 IOpenClass[] callParam,
                                 ICastFactory castFactory,
                                 boolean vararg,
                                 boolean allowMultiCallParams) {
            this.method = method;
            this.originalCallParams = originalCallParams;
            this.callParam = callParam;
            this.castFactory = castFactory;
            this.vararg = vararg;
            this.allowMultiCallParams = allowMultiCallParams;
            this.methodParam = method.getSignature().getParameterTypes();
            this.size = vararg ? originalCallParams.length + 1 : originalCallParams.length;
            this.multiCallParams = allowMultiCallParams ? new boolean[size] : null;
            this.paramCasts = new IOpenCast[size];
            this.castDistances = new Integer[size];
        }

        private Match calculate(IOpenClass varargElementType) {
            if (method instanceof JavaOpenMethod javaOpenMethod) {
                if (!matchJavaMethodParams(javaOpenMethod)) {
                    return NO_MATCH;
                }
            } else if (!matchParams()) {
                return NO_MATCH;
            }

            if (vararg && !matchVarargParams(varargElementType)) {
                return NO_MATCH;
            }

            int[] m = getSortedDistances();

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

        /**
         * Matches the call parameters to the parameters of a Java method. The generic parameter types and the generic
         * return type are resolved from the call parameters.
         */
        private boolean matchJavaMethodParams(JavaOpenMethod javaOpenMethod) {
            if (!vararg && isNullPassedAsNonNullLiteral(javaOpenMethod)) {
                return false;
            }
            Map<String, IOpenClass> genericTypes = new HashMap<>();
            int countOfParameters = javaOpenMethod.getParameterTypes().length;
            String[] typeNames = new String[countOfParameters];
            int[] arrayDims = new int[countOfParameters];
            if (!resolveGenericTypes(javaOpenMethod, genericTypes, typeNames, arrayDims)) {
                return false;
            }

            for (int i = 0; i < callParam.length; i++) {
                if (typeNames[i] != null && genericTypes.containsKey(typeNames[i])) {
                    if (!matchGenericParam(i, genericTypes.get(typeNames[i]), arrayDims[i])) {
                        return false;
                    }
                } else if (!matchJavaParam(i)) {
                    return false;
                }
            }
            return matchGenericReturnType(javaOpenMethod, genericTypes);
        }

        private boolean isNullPassedAsNonNullLiteral(JavaOpenMethod javaOpenMethod) {
            for (int i = 0; i < method.getSignature().getNumberOfParameters(); i++) {
                if (i < callParam.length && NullOpenClass
                        .isAnyNull(callParam[i]) && javaOpenMethod.getJavaMethod().getParameters()[i]
                        .isAnnotationPresent(NonNullLiteral.class)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Resolves the generic parameter types of a Java method from the call parameters. Writes the names and the
         * array dimensions of the generic parameter types to the given arrays.
         *
         * @return {@code false} when a call parameter does not fit the array dimension of its generic type
         */
        private boolean resolveGenericTypes(JavaOpenMethod javaOpenMethod,
                                            Map<String, IOpenClass> genericTypes,
                                            String[] typeNames,
                                            int[] arrayDims) {
            int i = 0;
            for (Type type : javaOpenMethod.getJavaMethod().getGenericParameterTypes()) {
                typeNames[i] = JavaGenericsUtils.getGenericTypeName(type);
                if (typeNames[i] != null) {
                    IOpenClass t = callParam[i];
                    if (NullOpenClass.isAnyNull(t)) {
                        continue;
                    }
                    arrayDims[i] = JavaGenericsUtils.getGenericTypeDim(type);
                    t = getGenericComponentType(i, t, arrayDims[i]);
                    if (t == null) {
                        return false;
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
            return true;
        }

        /**
         * Returns the component type of the call parameter at the array dimension of its generic type. A call
         * parameter with one dimension less is called with multiple values.
         *
         * @return the component type, or {@code null} when the call parameter does not fit the array dimension
         */
        private IOpenClass getGenericComponentType(int i, IOpenClass callParamType, int genericTypeDim) {
            IOpenClass t = callParamType;
            int arrayDim = genericTypeDim;
            while (t.isArray() && arrayDim > 0) {
                arrayDim--;
                t = t.getComponentClass();
            }
            if (arrayDim > 0) {
                if (arrayDim == 1 && allowMultiCallParams && callParam[i]
                        .isArray() && (!vararg || i != callParam.length - 1)) {
                    multiCallParams[i] = true;
                } else {
                    return null;
                }
            }
            return t;
        }

        /**
         * Matches a call parameter to a parameter of a generic type resolved from the call parameters.
         */
        private boolean matchGenericParam(int i, IOpenClass genericType, int arrayDim) {
            IOpenClass type = arrayDim > 0 ? genericType.getArrayType(arrayDim) : genericType;

            IOpenClass cp = callParam[i];
            if (allowMultiCallParams && multiCallParams[i]) {
                cp = cp.getComponentClass();
            }

            IOpenCast gCast = castFactory.getCast(cp, type);
            if (isInapplicable(gCast)) {
                if (canBeMultiCallParam(i, cp)) {
                    cp = cp.getComponentClass();
                    gCast = castFactory.getCast(cp, type);
                    if (isInapplicableToMultiCall(gCast)) {
                        return false;
                    }
                    multiCallParams[i] = true;
                } else {
                    return false;
                }
            }
            if (!NullOpenClass.isAnyNull(cp) && !Objects.equals(cp, type)) {
                return matchGenericTypeCast(i, gCast, type);
            } else {
                return matchCallParamCast(i, cp);
            }
        }

        /**
         * Sets the cast of a call parameter to its generic type, followed by the cast of the generic type to the
         * method parameter type when they differ.
         */
        private boolean matchGenericTypeCast(int i, IOpenCast gCast, IOpenClass type) {
            if (!Objects.equals(type, methodParam[i])) {
                IOpenCast cast = castFactory.getCast(type, methodParam[i]);
                if (cast == null || !cast.isImplicit()) {
                    return false;
                }
                paramCasts[i] = new CastsLinkageCast(gCast, cast);
            } else {
                paramCasts[i] = gCast;
            }
            castDistances[i] = paramCasts[i].getDistance();
            return true;
        }

        /**
         * Sets the cast of a call parameter to the method parameter type. When there is no implicit cast, an array
         * call parameter is tried to be called with multiple values.
         */
        private boolean matchCallParamCast(int i, IOpenClass callParamType) {
            IOpenClass cp = callParamType;
            if (!Objects.equals(cp, methodParam[i])) {
                paramCasts[i] = castFactory.getCast(cp, methodParam[i]);
                if (isInapplicable(paramCasts[i])) {
                    if (canBeMultiCallParam(i, cp)) {
                        cp = cp.getComponentClass();
                        paramCasts[i] = castFactory.getCast(cp, methodParam[i]);
                        if (isInapplicableToMultiCall(paramCasts[i])) {
                            return false;
                        }
                        multiCallParams[i] = true;
                    } else {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * Sets the cast of a call parameter to the parameter type of a Java method, which is not generic. When there
         * is no implicit cast, an array call parameter is tried to be called with multiple values.
         */
        private boolean matchJavaParam(int i) {
            IOpenClass cp = callParam[i];
            if (!Objects.equals(cp, methodParam[i])) {
                paramCasts[i] = getJavaParamCast(i, cp);
                if (isInapplicable(paramCasts[i])) {
                    if (canBeMultiCallParam(i, cp)) {
                        cp = cp.getComponentClass();
                        paramCasts[i] = getJavaParamCast(i, cp);
                        if (isInapplicableToMultiCall(paramCasts[i])) {
                            return false;
                        }
                        multiCallParams[i] = true;
                    } else {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * Returns the cast of a call parameter to the parameter type of a Java method. A parameter that accepts OpenL
         * types receives the type of the call parameter itself, which is cast to Object.
         */
        private IOpenCast getJavaParamCast(int i, IOpenClass cp) {
            if (IOpenClass.class.isAssignableFrom(methodParam[i].getInstanceClass()) && methodParam[i]
                    .getInstanceClass()
                    .isAssignableFrom(cp.getClass())) {
                return castFactory.getCast(cp, JavaOpenClass.OBJECT);
            } else {
                return castFactory.getCast(cp, methodParam[i]);
            }
        }

        /**
         * Finds the cast of the return type of a Java method to its generic return type resolved from the call
         * parameters.
         *
         * @return {@code false} when there is no such cast
         */
        private boolean matchGenericReturnType(JavaOpenMethod javaOpenMethod, Map<String, IOpenClass> genericTypes) {
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
                    return false;
                }
                returnType = type;
            }
            return true;
        }

        /**
         * Matches the call parameters to the parameters of a method that is not a Java method.
         */
        private boolean matchParams() {
            for (int i = 0; i < callParam.length; i++) {
                if (!matchParam(i)) {
                    return false;
                }
            }
            return true;
        }

        private boolean matchParam(int i) {
            IOpenClass cp = callParam[i];
            if (cp != methodParam[i]) {
                IOpenCast cast = castFactory.getCast(callParam[i], methodParam[i]);
                if (isInapplicable(cast)) {
                    if (allowMultiCallParams && cp.isArray() && !multiCallParams[i]) {
                        cp = cp.getComponentClass();
                        cast = castFactory.getCast(cp, methodParam[i]);
                        if (isInapplicableToMultiCall(cast)) {
                            return false;
                        }
                        multiCallParams[i] = true;
                    } else {
                        return false;
                    }
                }
                paramCasts[i] = cast;
            }
            return true;
        }

        /**
         * Sets the casts of the vararg call parameters to the vararg element type.
         */
        private boolean matchVarargParams(IOpenClass varargElementType) {
            for (int i = callParam.length - 1; i < size - 1; i++) {
                if (varargElementType != originalCallParams[i]) {
                    IOpenCast cast = castFactory.getCast(originalCallParams[i], varargElementType);
                    if (cast == null || !cast.isImplicit()) {
                        return false;
                    }
                    paramCasts[i + 1] = cast;
                }
            }
            return true;
        }

        private int[] getSortedDistances() {
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
            return m;
        }

        /**
         * Checks whether the cast cannot pass a call parameter. A one element array cast cannot pass it when the call
         * parameter may be called with multiple values instead.
         */
        private boolean isInapplicable(IOpenCast cast) {
            return cast == null || !cast.isImplicit() || allowMultiCallParams && cast instanceof IArrayOneElementCast;
        }

        /**
         * Checks whether the cast cannot pass the elements of a call parameter called with multiple values.
         */
        private static boolean isInapplicableToMultiCall(IOpenCast cast) {
            return cast == null || !cast.isImplicit() || cast instanceof IArrayOneElementCast;
        }

        /**
         * Checks whether an array call parameter can be called with multiple values. The last call parameter of a
         * vararg call cannot.
         */
        private boolean canBeMultiCallParam(int i, IOpenClass cp) {
            return allowMultiCallParams && cp.isArray() && !multiCallParams[i]
                    && (!vararg || i != callParam.length - 1);
        }

        private static IOpenClass unwrapPrimitiveClassIfNeeded(IOpenClass clazz) {
            if (clazz != null && clazz.getInstanceClass() != null && clazz.getInstanceClass().isPrimitive()) {
                return JavaOpenClass.getOpenClass(ClassUtils.primitiveToWrapper(clazz.getInstanceClass()));
            }
            return clazz;
        }
    }

    private static boolean isNoCastDistances(int[] m) {
        for (int value : m) {
            if (value != CastFactory.NO_CAST_DISTANCE) {
                return false;
            }
        }
        return true;
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

    /**
     * Returns the i-th greatest of the sorted values, or the default value when there are not so many values.
     */
    private static int getFromEnd(int[] sortedValues, int i, int defaultValue) {
        return i < sortedValues.length ? sortedValues[sortedValues.length - 1 - i] : defaultValue;
    }

    private static class Match {
        @Getter
        private final IOpenMethod method;
        @Getter
        private final IOpenClass[] callParams;
        private final IOpenClass[] originalCallParams;
        @Getter
        private final IOpenCast[] paramCasts;
        @Getter
        private final boolean[] multiCallParams;
        @Getter
        private final IOpenClass varargElementType;
        @Getter
        private final IOpenCast returnCast;
        @Getter
        private final IOpenClass returnType;
        @Getter
        private final int[] sortedDistances;
        private int[] sortedDims;
        @Getter
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
                        ret[i] = getVariableArityParameter(i, lastParameter);
                        i++;
                    }
                }
                mostSpecificParamsToCompare = ret;
            }
            return mostSpecificParamsToCompare;
        }

        private IOpenClass getVariableArityParameter(int i, IOpenClass lastParameter) {
            if (i < method.getSignature().getNumberOfParameters() - (vararg ? 1 : 0)) {
                return method.getSignature().getParameterType(i);
            } else if (vararg) {
                if (getMethod().getSignature().getNumberOfParameters() > originalCallParams.length) {
                    return NullOpenClass.the;
                } else {
                    return lastParameter.getComponentClass();
                }
            } else {
                return lastParameter;
            }
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
                return isMoreSpecificWithSameArgTypes(other);
            } else {
                return true;
            }
        }

        /**
         * Compares with a match of the same argument types. A non vararg match is more specific than a vararg one.
         * Otherwise, a method declared in a subclass is more specific.
         */
        private boolean isMoreSpecificWithSameArgTypes(Match other) {
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
        }

        public int[] getSortedDims() {
            if (sortedDims == null) {
                IOpenClass[] variableArityParameters = getVariableArityParameters();
                int[] dims = new int[variableArityParameters.length];
                for (int i = 0; i < variableArityParameters.length; i++) {
                    if (i < originalCallParams.length) {
                        if (!NullOpenClass.isAnyNull(originalCallParams[i])) {
                            int cpDim = getTypeDim(originalCallParams[i]);
                            int dim = getTypeDim(variableArityParameters[i]);
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

        private static int getTypeDim(IOpenClass openClass) {
            int dim = 0;
            while (openClass.isArray()) {
                openClass = openClass.getComponentClass();
                dim++;
            }
            return dim;
        }
    }

    private static IMethodCaller findCastingMethod(final String name,
                                                   IOpenClass[] callParams,
                                                   ICastFactory castFactory,
                                                   Iterable<IOpenMethod> methods,
                                                   boolean allowMultiCallParams) throws AmbiguousMethodException {
        final int nParams = callParams.length;
        Iterable<IOpenMethod> filtered = methods == null ? List.of()
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
        var bestMatches = new BestMatches(allowMultiCallParams);
        LazyVarargTypeCalculator lazyVarargTypeCalculatorClosestClass = new LazyVarargTypeCalculator(callParams,
                castFactory::findClosestClass);
        for (IOpenMethod method : filtered) {
            if (!isSuitableMethod(method, callParams, castFactory)) {
                continue;
            }
            var matches = calcMatches(method,
                    callParams,
                    castFactory,
                    lazyVarargTypeCalculatorClosestClass,
                    allowMultiCallParams);
            bestMatches.addMethodMatches(matches);
        }
        List<Match> matchingResult = bestMatches.matchingResult;

        IMethodCaller methodCaller = null;
        Match selectedMatch = null;
        switch (matchingResult.size()) {
            case 0:
                break;
            case 1:
                selectedMatch = matchingResult.getFirst();
                methodCaller = buildMethodCaller(selectedMatch);
                break;
            default:
                int mostSpecificMatchIndex = findMostSpecific(name, callParams, matchingResult, castFactory);
                selectedMatch = matchingResult.get(mostSpecificMatchIndex);
                methodCaller = buildMostSpecificMethodCaller(selectedMatch, callParams);
        }
        if (methodCaller != null) {
            return wrapMethodCaller(methodCaller, selectedMatch, callParams, castFactory);
        }
        return null;
    }

    /**
     * Matches the call parameters to the method as a call with the same number of parameters, and as a vararg call.
     */
    private static List<Match> calcMatches(IOpenMethod method,
                                           IOpenClass[] callParams,
                                           ICastFactory castFactory,
                                           LazyVarargTypeCalculator lazyVarargTypeCalculatorClosestClass,
                                           boolean allowMultiCallParams) {
        List<Match> matches = new ArrayList<>();
        if (method.getSignature().getNumberOfParameters() == callParams.length && isNonVarargSupported(method)) {
            Match noVarargMatch = calcMatch(method,
                    callParams,
                    callParams,
                    castFactory,
                    false,
                    null,
                    allowMultiCallParams);
            matches.add(noVarargMatch);
        }
        if (isVarargsSupported(method)) {
            boolean isGeneric = isGenericVararg(method);
            if (isGeneric) {
                IOpenClass varargElementType = lazyVarargTypeCalculatorClosestClass
                        .getElementType(method.getSignature().getNumberOfParameters() - 1);
                if (varargElementType != null) {
                    matches.add(calcMatch(method,
                            callParams,
                            lazyVarargTypeCalculatorClosestClass
                                    .getVarargMethodCallParams(method.getSignature().getNumberOfParameters() - 1),
                            castFactory,
                            true,
                            varargElementType,
                            allowMultiCallParams));
                }
            } else {
                int lastParameterIndex = method.getSignature().getNumberOfParameters() - 1;
                IOpenClass varargElementType = method.getSignature().getParameterTypes()[lastParameterIndex]
                        .getComponentClass();
                IOpenClass[] varargMethodCallParams = new IOpenClass[method.getSignature().getNumberOfParameters()];
                System.arraycopy(callParams, 0, varargMethodCallParams, 0, lastParameterIndex);
                varargMethodCallParams[lastParameterIndex] = (varargElementType == null || NullOpenClass
                        .isAnyNull(varargElementType)) ? varargElementType
                        : varargElementType.getAggregateInfo()
                        .getIndexedAggregateType(varargElementType);
                matches.add(calcMatch(method,
                        callParams,
                        varargMethodCallParams,
                        castFactory,
                        true,
                        varargElementType,
                        allowMultiCallParams));
            }
        }
        return matches;
    }

    /**
     * Collects the best matches of the methods. A match is better when it has fewer one element array casts, then
     * fewer parameters called with multiple values, then lower dimensions and cast distances.
     */
    private static final class BestMatches {
        private final List<Match> matchingResult = new ArrayList<>();
        private final boolean allowMultiCallParams;
        private Match bestMethodMatch = NO_MATCH;
        private long bestOneElementToArrayCastCount = Integer.MAX_VALUE;
        private int bestMultiCallParamsCount;

        private BestMatches(boolean allowMultiCallParams) {
            this.allowMultiCallParams = allowMultiCallParams;
            this.bestMultiCallParamsCount = allowMultiCallParams ? Integer.MAX_VALUE : 0;
        }

        /**
         * Adds the matches of one method. Of the matches as good as the best ones, only the first is added.
         */
        private void addMethodMatches(List<Match> matches) {
            boolean f = false;
            for (Match match : matches) {
                if (match == NO_MATCH) {
                    continue;
                }
                long oneElementToArrayCastCount = Arrays.stream(match.paramCasts)
                        .filter(IOneElementArrayCast.class::isInstance)
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
                } else if (oneElementToArrayCastCount == bestOneElementToArrayCastCount
                        && multiCallParamsHolderCount == bestMultiCallParamsCount && eq(match, bestMethodMatch) && !f) {
                    matchingResult.add(match);
                    f = true;
                }
            }
        }

        private static int countMultiCallParams(Match match) {
            if (match == NO_MATCH || match.multiCallParams == null) {
                return Integer.MAX_VALUE;
            }
            return countTrues(match.multiCallParams);
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
                int p1 = getFromEnd(dims1, i, 0);
                int p2 = getFromEnd(dims2, i, 0);

                if (p1 != p2) {
                    return p1 < p2;
                }
            }

            // FIXME REMOVE IT
            if (match.isVararg() && !bestMethodMatch.isVararg()) {
                return false;
            }
            if (bestMethodMatch.isVararg() && !match.isVararg()) {
                return true;
            }
            // END FIXME

            int[] d1 = match.getSortedDistances();
            int[] d2 = bestMethodMatch.getSortedDistances();
            x = Math.max(d1.length, d2.length);
            for (int i = 0; i < x; i++) {
                int p1 = getFromEnd(d1, i, CastFactory.NO_CAST_DISTANCE);
                int p2 = getFromEnd(d2, i, CastFactory.NO_CAST_DISTANCE);

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
    }

    private static IMethodCaller buildMethodCaller(Match selectedMatch) {
        IOpenMethod m = selectedMatch.getMethod();
        if (!isNoCastDistances(selectedMatch.getSortedDistances())) {
            IOpenCast[] paramCasts = getParamCastsAndTruncateIfNeed(selectedMatch);
            CastingMethodCaller methodCaller1 = new CastingMethodCaller(m, paramCasts);
            return buildMethod(selectedMatch.getReturnCast(),
                    selectedMatch.getReturnType(),
                    m,
                    methodCaller1);
        } else {
            return buildMethod(selectedMatch.getReturnCast(), selectedMatch.getReturnType(), m, m);
        }
    }

    /**
     * Builds the caller of the most specific method. The method is called as is when its parameter types are the
     * call parameter types.
     */
    private static IMethodCaller buildMostSpecificMethodCaller(Match selectedMatch, IOpenClass[] callParams) {
        IOpenMethod method = selectedMatch.getMethod();
        boolean f = true;
        for (int i = 0; i < callParams.length; i++) {
            if (!callParams[i].equals(method.getSignature().getParameterType(i))) {
                f = false;
                break;
            }
        }
        if (f) {
            return method;
        } else {
            IOpenCast[] paramCasts = getParamCastsAndTruncateIfNeed(selectedMatch);
            CastingMethodCaller methodCaller1 = new CastingMethodCaller(method, paramCasts);
            return buildMethod(selectedMatch.getReturnCast(),
                    selectedMatch.getReturnType(),
                    selectedMatch.getMethod(),
                    methodCaller1);
        }
    }

    /**
     * Wraps the caller of the selected method to pass the vararg parameters, to apply the method search tuner of a
     * Java method, and to call the method with multiple values of the parameters.
     */
    private static IMethodCaller wrapMethodCaller(IMethodCaller methodCaller,
                                                  Match selectedMatch,
                                                  IOpenClass[] callParams,
                                                  ICastFactory castFactory) {
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

    private static boolean isSuitableMethod(IOpenMethod method, IOpenClass[] callParams, ICastFactory castFactory) {
        if (method instanceof JavaOpenMethod javaOpenMethod) {
            Method javaMethod = javaOpenMethod.getJavaMethod();
            MethodSearchTuner methodSearchTuner = javaMethod.getAnnotation(MethodSearchTuner.class);
            if (methodSearchTuner != null) {
                Class<? extends MethodFilter> clazz = methodSearchTuner.methodFilter();
                if (clazz != MethodSearchTuner.DefaultMethodFilter.class) {
                    try {
                        MethodFilter methodFilter = clazz.getDeclaredConstructor().newInstance();
                        return methodFilter.predicate(javaOpenMethod, callParams, castFactory);
                    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
                             | InvocationTargetException ignored) {
                        // a filter that cannot be created does not restrict the method
                    }
                }
            }
        }
        return true;
    }

    private static boolean isGenericVararg(IOpenMethod method) {
        if (method instanceof JavaOpenMethod javaOpenMethod && method.getSignature().getNumberOfParameters() > 0 && method
                .getSignature()
                .getParameterTypes()[method.getSignature().getNumberOfParameters() - 1].isArray()) {
            Type lastParameterType = javaOpenMethod.getJavaMethod()
                    .getGenericParameterTypes()[javaOpenMethod.getNumberOfParameters() - 1];
            return JavaGenericsUtils.getGenericTypeName(lastParameterType) != null;
        }
        return false;
    }

    private static IMethodCaller processJavaAnnotationsOnMethod(IOpenClass[] callParams,
                                                                ICastFactory castFactory,
                                                                IMethodCaller methodCaller,
                                                                Match selectedMatch) {
        JavaOpenMethod javaOpenMethod = (JavaOpenMethod) selectedMatch.getMethod();
        Method javaMethod = javaOpenMethod.getJavaMethod();
        MethodSearchTuner methodSearchTuner = javaMethod.getAnnotation(MethodSearchTuner.class);
        if (methodSearchTuner != null) {
            Class<? extends MethodCallerWrapper> clazz = methodSearchTuner.wrapper();
            if (clazz != MethodSearchTuner.DefaultMethodCallerWrapper.class) {
                try {
                    MethodCallerWrapper methodCallerWrapper = clazz.getDeclaredConstructor().newInstance();
                    methodCaller = methodCallerWrapper.handle(methodCaller, javaOpenMethod, callParams, castFactory);
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
                         | InvocationTargetException ignored) {
                    // a wrapper that cannot be created leaves the method caller as it is
                }
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

    private static boolean isVarargsSupported(IOpenMethod method) {
        if (method.getSignature().getNumberOfParameters() > 0 && method.getSignature()
                .getParameterTypes()[method.getSignature().getNumberOfParameters() - 1].isArray()) {
            if (method instanceof JavaOpenMethod javaOpenMethod) {
                if (javaOpenMethod.getJavaMethod().isAnnotationPresent(IgnoreVarargsMatching.class)) {
                    return false;
                }
                return !javaOpenMethod.getJavaMethod()
                        .getDeclaringClass()
                        .isAnnotationPresent(IgnoreVarargsMatching.class);
            }
            return true;
        }
        return false;
    }

    private static boolean isNonVarargSupported(IOpenMethod method) {
        if (method instanceof JavaOpenMethod javaOpenMethod) {
            if (javaOpenMethod.getJavaMethod().isAnnotationPresent(IgnoreNonVarargsMatching.class)) {
                return false;
            }
            return !javaOpenMethod.getJavaMethod()
                    .getDeclaringClass()
                    .isAnnotationPresent(IgnoreNonVarargsMatching.class);
        }
        return true;
    }

    private static class LazyVarargTypeCalculator {
        private final IOpenClass[] callParams;
        private final BinaryOperator<IOpenClass> func;
        private final IOpenClass[] varArgElementTypes;
        private int lastCalculated;
        private IOpenClass lastVarArgElementType;
        private final IOpenClass[][] varargMethodCallParamsCache;

        public LazyVarargTypeCalculator(IOpenClass[] callParams, BinaryOperator<IOpenClass> func) {
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
     * @param name    The name of the method.
     * @param params  Argument types of the method.
     * @param matches All matching methods for this argument types.
     * @param casts   OpenL cast factory.
     * @return The most specific method from matching methods collection.
     * @throws AmbiguousMethodException Exception will be thrown if most specific method cannot be determined.
     * @see <a href= "http://java.sun.com/docs/books/jls/second_edition/html/expressions.doc.html#18428" >java
     * documentation </a >
     */
    private static int findMostSpecific(String name,
                                        IOpenClass[] params,
                                        List<Match> matches,
                                        ICastFactory casts) throws AmbiguousMethodException {
        List<Integer> moreSpecificIndexes = new ArrayList<>();
        for (int i = 0; i < matches.size(); i++) {
            if (isMoreSpecificThanOthers(matches.get(i), matches, casts)) {
                moreSpecificIndexes.add(i);
            }
        }

        if (moreSpecificIndexes.size() == 1) {
            return moreSpecificIndexes.getFirst();
        } else {
            return findLeastPenalized(name, params, matches, moreSpecificIndexes);
        }
    }

    private static boolean isMoreSpecificThanOthers(Match res, List<Match> matches, ICastFactory casts) {
        for (Match next : matches) {
            if (res != next && !res.isMoreSpecific(next, casts)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Chooses the method with the lowest penalties among the more specific ones. See {@link #countPenalties}.
     *
     * @throws AmbiguousMethodException when several methods or none have the lowest penalties
     */
    private static int findLeastPenalized(String name,
                                          IOpenClass[] params,
                                          List<Match> matches,
                                          List<Integer> moreSpecificIndexes) throws AmbiguousMethodException {
        List<Integer> mostSpecificIndexes = new ArrayList<>();
        int best1 = Integer.MAX_VALUE;
        int best2 = Integer.MAX_VALUE;
        for (Integer index : moreSpecificIndexes) {
            var penalties = countPenalties(matches.get(index).getMethod(), params);
            int penalty1 = penalties.penalty1();
            int penalty2 = penalties.penalty2();
            if (penalty1 < best1) {
                best1 = penalty1;
                best2 = penalty2;
                mostSpecificIndexes.clear();
                mostSpecificIndexes.add(index);
            } else if (penalty1 == best1) {
                if (penalty2 < best2) {
                    best2 = penalty2;
                    mostSpecificIndexes.clear();
                    mostSpecificIndexes.add(index);
                } else if (penalty2 == best2) {
                    mostSpecificIndexes.add(index);
                }
            }
        }

        int countOfFoundMethods = mostSpecificIndexes.size();
        return switch (countOfFoundMethods) {
            case 1 -> mostSpecificIndexes.getFirst();
            case 0 -> throw new AmbiguousMethodException(name,
                    params,
                    matches.stream().map(Match::getMethod).collect(Collectors.toList()));
            default -> throw new AmbiguousMethodException(name,
                    params,
                    moreSpecificIndexes.stream().map(matches::get).map(Match::getMethod).collect(Collectors.toList()));
        };
    }

    /**
     * Counts the penalties of a method that has as many parameters as the call; other methods have no penalties.
     *
     * <p>The first penalty counts the primitive parameters that receive a NULL literal or a non-primitive value. The
     * second one counts the parameters that receive a NULL literal or a value of different primitiveness.
     */
    private static Penalties countPenalties(IOpenMethod m, IOpenClass[] params) {
        int penalty1 = 0;
        int penalty2 = 0;
        if (m.getSignature().getNumberOfParameters() != params.length) {
            return new Penalties(penalty1, penalty2);
        }
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
        return new Penalties(penalty1, penalty2);
    }

    private record Penalties(int penalty1, int penalty2) {
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
        if (factory instanceof ADynamicClass class1) {
            caller = class1.getConstructor(params, true);
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
        if (factory instanceof ADynamicClass aDynamicClass) {
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
