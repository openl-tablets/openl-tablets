package org.openl.binding.impl.method;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import org.openl.binding.ICastFactory;
import org.openl.binding.IMethodFactory;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.impl.cast.CastsLinkageCast;
import org.openl.binding.impl.cast.IOneElementArrayCast;
import org.openl.binding.impl.cast.IOpenCast;
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
import org.openl.util.OpenClassUtils;

/**
 * @author snshor, Marat Kamalov
 *
 */
public final class MethodSearch {

    private MethodSearch() {
    }

    static final int[] NO_MATCH = new int[0];

    private static int[] calcMatch(JavaOpenMethod method,
            IOpenClass[] methodParam,
            IOpenClass[] callParam,
            ICastFactory castFactory,
            IOpenCast[] castHolder,
            IOpenCast[] returnCastHolder,
            IOpenClass[] returnTypeHolder) {
        Integer[] castHolderDistance = new Integer[callParam.length];
        if (method != null) {
            Map<String, IOpenClass> genericTypes = new HashMap<>();
            String[] typeNames = new String[method.getParameterTypes().length];
            int[] arrayDims = new int[method.getParameterTypes().length];
            int i = 0;
            for (Type type : method.getJavaMethod().getGenericParameterTypes()) {
                typeNames[i] = JavaGenericsUtils.getGenericTypeName(type);
                if (typeNames[i] != null) {
                    arrayDims[i] = JavaGenericsUtils.getGenericTypeDim(type);
                    int arrayDim = arrayDims[i];
                    IOpenClass t = callParam[i];

                    if (t.getInstanceClass() != null && t.getInstanceClass().isPrimitive()) {
                        t = JavaOpenClass.getOpenClass(ClassUtils.primitiveToWrapper(t.getInstanceClass()));
                    }

                    while (t.isArray() && arrayDim > 0) {
                        arrayDim--;
                        t = t.getComponentClass();
                    }
                    if (arrayDim > 0) {
                        return NO_MATCH;
                    }
                    t = unwrapPrimitiveClassIfNeeded(t);
                    if (genericTypes.containsKey(typeNames[i])) {
                        IOpenClass existedType = genericTypes.get(typeNames[i]);
                        IOpenClass clazz = castFactory.findClosestClass(t, existedType);
                        if (clazz != null) {
                            genericTypes.put(typeNames[i], unwrapPrimitiveClassIfNeeded(clazz));
                        } else {
                            return NO_MATCH;
                        }
                    } else {
                        genericTypes.put(typeNames[i], t);
                    }
                }
                i++;
            }

            for (i = 0; i < callParam.length; i++) {
                if (typeNames[i] != null) {
                    IOpenClass type = genericTypes.get(typeNames[i]);
                    type = arrayDims[i] > 0 ? type.getArrayType(arrayDims[i]) : type;

                    IOpenCast tCast = castFactory.getCast(callParam[i], type);
                    if (tCast == null || !tCast.isImplicit()) {
                        return NO_MATCH;
                    }

                    if (callParam[i] != type) {
                        IOpenCast gCast = castFactory.getCast(callParam[i], type);
                        if (gCast == null || !gCast.isImplicit()) {
                            return NO_MATCH;
                        }
                        if (!Objects.equals(type, methodParam[i])) {
                            IOpenCast cast = castFactory.getCast(type, methodParam[i]);
                            if (cast == null || !cast.isImplicit()) {
                                return NO_MATCH;
                            }
                            castHolder[i] = new CastsLinkageCast(gCast, cast);
                        } else {
                            castHolder[i] = gCast;
                        }
                        castHolderDistance[i] = castHolder[i].getDistance();
                    } else {
                        if (callParam[i] != methodParam[i]) {
                            castHolder[i] = castFactory.getCast(callParam[i], methodParam[i]);
                            if (castHolder[i] == null || !castHolder[i].isImplicit()) {
                                return NO_MATCH;
                            }
                        }
                    }
                } else {
                    if (callParam[i] != methodParam[i]) {
                        castHolder[i] = castFactory.getCast(callParam[i], methodParam[i]);
                        if (castHolder[i] == null || !castHolder[i].isImplicit()) {
                            return NO_MATCH;
                        }
                    }
                }
            }

            String returnType = JavaGenericsUtils.getGenericTypeName(method.getJavaMethod().getGenericReturnType());

            if (returnType != null && genericTypes.containsKey(returnType)) {
                int dim = JavaGenericsUtils.getGenericTypeDim(method.getJavaMethod().getGenericReturnType());
                IOpenClass type = genericTypes.get(returnType);
                if (dim > 0) {
                    type = type.getArrayType(dim);
                }
                IOpenCast returnCast = castFactory.getCast(method.getType(), type);
                if (returnCast == null) {
                    return NO_MATCH;
                }
                returnCastHolder[0] = returnCast;
                returnTypeHolder[0] = type;
            }
        } else {
            for (int i = 0; i < callParam.length; i++) {
                if (callParam[i] != methodParam[i]) {
                    IOpenCast cast = castFactory.getCast(callParam[i], methodParam[i]);
                    if (cast == null || !cast.isImplicit()) {
                        return NO_MATCH;
                    }
                    castHolder[i] = cast;
                }
            }
        }

        int[] m = new int[callParam.length];

        for (int i = 0; i < callParam.length; i++) {
            if (castHolder[i] != null) {
                if (castHolderDistance[i] == null) {
                    m[i] = castHolder[i].getDistance();
                } else {
                    m[i] = castHolderDistance[i];
                }
            }
        }

        if (castHolder.length > 0 && castHolder[castHolder.length - 1] instanceof IOneElementArrayCast) {
            return NO_MATCH;
        }

        return m;
    }

    private static IOpenClass unwrapPrimitiveClassIfNeeded(IOpenClass clazz) {
        if (clazz != null && clazz.getInstanceClass() != null && clazz.getInstanceClass().isPrimitive()) {
            return JavaOpenClass.getOpenClass(ClassUtils.primitiveToWrapper(clazz.getInstanceClass()));
        }
        return clazz;
    }

    private static boolean zeroCasts(int[] m) {
        for (int value : m) {
            if (value != 0) {
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

    private static boolean lq(IOpenMethod method,
            List<IOpenMethod> matchingMethods,
            IOpenClass[] callParams,
            int[] m1,
            int[] m2) {
        if (matchingMethods == null || matchingMethods.isEmpty()) {
            return true;
        }
        IOpenMethod m = matchingMethods.get(0);
        int[] dims1 = new int[method.getSignature().getNumberOfParameters()];
        int[] dims2 = new int[method.getSignature().getNumberOfParameters()];
        for (int i = 0; i < method.getSignature().getNumberOfParameters(); i++) {
            int cpDim = getTypeDim(callParams[i]);
            if (!NullOpenClass.isAnyNull(callParams[i])) {
                IOpenClass openClass = method.getSignature().getParameterType(i);
                int dim = 0;
                while (openClass.isArray()) {
                    openClass = openClass.getComponentClass();
                    dim++;
                }
                dims1[i] = Math.abs(dim - cpDim);
                dim = 0;
                openClass = m.getSignature().getParameterType(i);
                while (openClass.isArray()) {
                    openClass = openClass.getComponentClass();
                    dim++;
                }
                dims2[i] = Math.abs(dim - cpDim);
            }
        }
        Arrays.sort(dims1);
        Arrays.sort(dims2);
        for (int i = dims1.length - 1; i >= 0; i--) {
            if (dims1[i] != dims2[i]) {
                return dims1[i] < dims2[i];
            }
        }

        if (m1 == NO_MATCH) {
            return false;
        }
        if (m2 == NO_MATCH) {
            return true;
        }
        int[] d1 = m1.clone();
        int[] d2 = m2.clone();
        Arrays.sort(d1);
        Arrays.sort(d2);
        for (int i = d1.length - 1; i >= 0; i--) {
            if (d1[i] < d2[i]) {
                return true;
            }
            if (d1[i] > d2[i]) {
                return false;
            }
        }
        return false;
    }

    private static boolean eq(int[] distances1, int[] distances2) {
        if (distances1 == distances2) {
            return true;
        }
        if (distances1.length != distances2.length) {
            return false;
        }
        int[] d1 = distances1.clone();
        int[] d2 = distances2.clone();
        Arrays.sort(d1);
        Arrays.sort(d2);
        for (int i = d1.length - 1; i >= 0; i--) {
            if (d1[i] != d2[i]) {
                return false;
            }
        }
        return true;
    }

    private static IMethodCaller findCastingMethod(final String name,
            IOpenClass[] params,
            ICastFactory castFactory,
            Iterable<IOpenMethod> methods) throws AmbiguousMethodException {
        final int nParams = params.length;
        Iterable<IOpenMethod> filtered = methods == null ? Collections.emptyList()
                                                         : CollectionUtils.findAll(methods,
                                                             method -> method.getName()
                                                                 .equals(name) && method.getSignature()
                                                                     .getParameterTypes().length == nParams);

        List<IOpenMethod> matchingMethods = new ArrayList<>();
        List<IOpenCast[]> matchingMethodsCastHolder = new ArrayList<>();
        List<IOpenCast> matchingMethodsReturnCast = new ArrayList<>();
        List<IOpenClass> matchingMethodsReturnType = new ArrayList<>();
        int[] bestMatch = NO_MATCH;
        for (IOpenMethod method : filtered) {
            IOpenCast[] castHolder = new IOpenCast[nParams];
            IOpenCast[] returnCastHolder = new IOpenCast[1];
            IOpenClass[] returnTypeHolder = new IOpenClass[1];
            int[] match;
            if (method instanceof JavaOpenMethod) { // Process Java Generics
                JavaOpenMethod javaOpenMethod = (JavaOpenMethod) method;
                match = calcMatch(javaOpenMethod,
                    method.getSignature().getParameterTypes(),
                    params,
                    castFactory,
                    castHolder,
                    returnCastHolder,
                    returnTypeHolder);
            } else {
                match = calcMatch(null,
                    method.getSignature().getParameterTypes(),
                    params,
                    castFactory,
                    castHolder,
                    returnCastHolder,
                    returnTypeHolder);
            }
            if (match == NO_MATCH) {
                continue;
            }
            if (lq(method, matchingMethods, params, match, bestMatch)) {
                bestMatch = match;
                matchingMethods.clear();
                matchingMethodsCastHolder.clear();
                matchingMethodsReturnCast.clear();
                matchingMethodsReturnType.clear();
                matchingMethods.add(method);
                matchingMethodsCastHolder.add(castHolder);
                matchingMethodsReturnCast.add(returnCastHolder[0]);
                matchingMethodsReturnType.add(returnTypeHolder[0]);
                continue;
            }

            if (eq(match, bestMatch)) {
                matchingMethods.add(method);
                matchingMethodsCastHolder.add(castHolder);
                matchingMethodsReturnCast.add(returnCastHolder[0]);
                matchingMethodsReturnType.add(returnTypeHolder[0]);
            }
        }

        switch (matchingMethods.size()) {
            case 0:
                return null;
            case 1:
                IOpenMethod m = matchingMethods.get(0);
                if (!zeroCasts(bestMatch)) {
                    CastingMethodCaller methodCaller = new CastingMethodCaller(m, matchingMethodsCastHolder.get(0));
                    return buildMethod(matchingMethodsReturnCast.get(0),
                        matchingMethodsReturnType.get(0),
                        m,
                        methodCaller);
                } else {
                    return buildMethod(matchingMethodsReturnCast.get(0), matchingMethodsReturnType.get(0), m, m);
                }
            default:
                IOpenMethod mostSpecificMethod = findMostSpecificMethod(name, params, matchingMethods, castFactory);
                boolean f = true;
                for (int i = 0; i < nParams; i++) {
                    if (!params[i].equals(mostSpecificMethod.getSignature().getParameterType(i))) {
                        f = false;
                        break;
                    }
                }
                if (f) {
                    return mostSpecificMethod;
                } else {
                    int k = 0;
                    for (int i = 0; i < matchingMethods.size(); i++) {
                        if (matchingMethods.get(i) == mostSpecificMethod) {
                            k = i;
                            break;
                        }
                    }
                    CastingMethodCaller methodCaller = new CastingMethodCaller(mostSpecificMethod,
                        matchingMethodsCastHolder.get(k));
                    IOpenCast c = matchingMethodsReturnCast.get(k);
                    IOpenClass t = matchingMethodsReturnType.get(k);
                    if (c != null && t != mostSpecificMethod.getType()) {
                        return new AutoCastableResultOpenMethod(methodCaller, t, c);
                    } else {
                        return methodCaller;
                    }
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

    private static IMethodCaller findVarArgMethod(final String name,
            IOpenClass[] params,
            ICastFactory castFactory,
            Iterable<IOpenMethod> methods,
            BiFunction<IOpenClass, IOpenClass, IOpenClass> func) throws AmbiguousMethodException {
        if (methods.iterator().hasNext()) {
            for (int i = params.length - 1; i >= 0; i--) {
                IOpenClass[] args = new IOpenClass[i + 1];
                System.arraycopy(params, 0, args, 0, i);
                IOpenClass varArgType = params[i];
                for (int j = i + 1; j < params.length; j++) {
                    varArgType = func.apply(varArgType, params[j]);
                    if (varArgType == null) {
                        break;
                    }
                }
                if (varArgType == null) {
                    continue;
                }
                if (NullOpenClass.isAnyNull(varArgType)) {
                    args[i] = varArgType;
                } else {
                    args[i] = varArgType.getAggregateInfo().getIndexedAggregateType(varArgType);
                }

                IMethodCaller matchedMethod = findCastingMethod(name, args, castFactory, methods);
                if (matchedMethod != null) {
                    IOpenCast[] parameterCasts = new IOpenCast[params.length - i];
                    for (int j = 0; j < params.length - i; j++) {
                        parameterCasts[j] = castFactory.getCast(params[i + j], varArgType);
                    }
                    if (NullOpenClass.isAnyNull(varArgType)) {
                        int lastParameterIndex = matchedMethod.getMethod().getSignature().getNumberOfParameters() - 1;
                        return new VarArgsOpenMethod(matchedMethod,
                            matchedMethod.getMethod()
                                .getSignature()
                                .getParameterType(lastParameterIndex)
                                .getComponentClass()
                                .getInstanceClass(),
                            i,
                            parameterCasts);
                    } else {
                        return new VarArgsOpenMethod(matchedMethod, varArgType.getInstanceClass(), i, parameterCasts);
                    }
                }
            }
        }
        return null;
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
     * @param matchingMethods All matching methods for this argument types.
     * @param casts OpenL cast factory.
     *
     * @return The most specific method from matching methods collection.
     *
     * @throws AmbiguousMethodException Exception will be thrown if most specific method cannot be determined.
     */
    private static IOpenMethod findMostSpecificMethod(String name,
            IOpenClass[] params,
            List<IOpenMethod> matchingMethods,
            ICastFactory casts) throws AmbiguousMethodException {
        List<IOpenMethod> moreSpecificMethods = new ArrayList<>();
        for (IOpenMethod res : matchingMethods) {
            boolean f = true;
            for (IOpenMethod next : matchingMethods) {
                if (res != next && !isMoreSpecificMethod(res, next, params, casts)) {
                    f = false;
                    break;
                }
            }
            if (f) {
                moreSpecificMethods.add(res);
            }
        }

        if (moreSpecificMethods.size() == 1) {
            return moreSpecificMethods.get(0);
        } else {
            List<IOpenMethod> mostSpecificMethods = new ArrayList<>();
            int best1 = Integer.MAX_VALUE;
            int best2 = Integer.MAX_VALUE;
            for (IOpenMethod m : moreSpecificMethods) {
                int penalty1 = 0;
                int penalty2 = 0;
                if (m.getSignature().getNumberOfParameters() == params.length) {
                    for (int i = 0; i < params.length; i++) {
                        if (!params[i].getInstanceClass().isPrimitive() && m.getSignature()
                            .getParameterType(i)
                            .getInstanceClass()
                            .isPrimitive()) {
                            penalty1++;
                        }
                        if (params[i].getInstanceClass()
                            .isPrimitive() != m.getSignature().getParameterType(i).getInstanceClass().isPrimitive()) {
                            penalty2++;
                        }
                    }
                }
                if (penalty1 < best1) {
                    best1 = penalty1;
                    best2 = penalty2;
                    mostSpecificMethods.clear();
                    mostSpecificMethods.add(m);
                } else {
                    if (penalty1 == best1) {
                        if (penalty2 < best2) {
                            best2 = penalty2;
                            mostSpecificMethods.clear();
                            mostSpecificMethods.add(m);
                        } else {
                            if (penalty2 == best2) {
                                mostSpecificMethods.add(m);
                            }
                        }
                    }
                }
            }

            int countOfFoundMethods = mostSpecificMethods.size();
            if (countOfFoundMethods == 1) {
                return mostSpecificMethods.get(0);
            } else if (countOfFoundMethods == 0) {
                throw new AmbiguousMethodException(name, params, matchingMethods);
            } else {
                throw new AmbiguousMethodException(name, params, mostSpecificMethods);
            }
        }
    }

    private static boolean isMoreSpecificMethod(IOpenMethod first,
            IOpenMethod second,
            IOpenClass[] params,
            ICastFactory casts) {
        if (first.getSignature().getNumberOfParameters() != second.getSignature().getNumberOfParameters()) {
            return false;
        }
        boolean differenceInArgTypes = false;
        // more specific arg types
        for (int i = 0; i < first.getSignature().getNumberOfParameters(); i++) {
            IOpenClass firstArgType = first.getSignature().getParameterType(i);
            IOpenClass secondArgType = second.getSignature().getParameterType(i);
            if (!firstArgType.equals(secondArgType) && !NullOpenClass.isAnyNull(params[i])) {
                differenceInArgTypes = true;
                IOpenCast cast = casts.getCast(firstArgType, secondArgType);
                if (cast == null || !cast.isImplicit()) {
                    return false;
                }
            }
        }
        if (!differenceInArgTypes) {
            // more specific declaring class
            IOpenClass firstDeclaringClass = first.getDeclaringClass();
            IOpenClass secondDeclaringClass = second.getDeclaringClass();
            return !firstDeclaringClass.equals(secondDeclaringClass) && secondDeclaringClass
                .isAssignableFrom(firstDeclaringClass);
        } else {
            return true;
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
            IMethodFactory factory) throws AmbiguousMethodException {
        return findMethod(name, params, castFactory, factory, false);
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
        return findCastingMethod("<init>", params, casts, factory.constructors());
    }

    public static IMethodCaller findMethod(String name,
            IOpenClass[] params,
            ICastFactory castFactory,
            IMethodFactory factory,
            boolean strictMatch) throws AmbiguousMethodException {
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
        if (params.length == 0 || castFactory == null) {
            return null;
        }
        if (!strictMatch) {
            return findMethod(name, params, castFactory, factory.methods(name));
        }
        return null;
    }

    public static IMethodCaller findMethod(String name,
            IOpenClass[] params,
            ICastFactory castFactory,
            Iterable<IOpenMethod> methods) throws AmbiguousMethodException {
        IMethodCaller caller = findCastingMethod(name, params, castFactory, methods);
        if (caller != null) {
            return caller;
        }
        Iterable<IOpenMethod> filtered = methods == null ? Collections.emptyList()
                                                         : CollectionUtils.findAll(methods,
                                                             method -> method.getName()
                                                                 .equals(name) && method.getSignature()
                                                                     .getNumberOfParameters() > 0 && method
                                                                         .getSignature()
                                                                         .getParameterType(method.getSignature()
                                                                             .getNumberOfParameters() - 1)
                                                                         .isArray());
        caller = findVarArgMethod(name, params, castFactory, filtered, OpenClassUtils::findParentClass);
        if (caller != null) {
            return caller;
        }
        return findVarArgMethod(name, params, castFactory, filtered, castFactory::findClosestClass);
    }
}
