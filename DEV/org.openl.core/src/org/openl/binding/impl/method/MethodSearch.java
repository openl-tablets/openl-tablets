package org.openl.binding.impl.method;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.binding.ICastFactory;
import org.openl.binding.IMethodFactory;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.impl.cast.CastsLinkageCast;
import org.openl.binding.impl.cast.IgnoredByMethodSearchOpenCast;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.NullOpenClass;
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
public class MethodSearch {

    static final int[] NO_MATCH = new int[0];

    private static int[] calcMatch(JavaOpenMethod method,
            IOpenClass[] methodParam,
            IOpenClass[] callParam,
            ICastFactory casts,
            IOpenCast[] castHolder,
            IOpenCast[] returnCastHolder,
            IOpenClass[] returnTypeHolder) {
        Integer[] castHolderDistance = new Integer[callParam.length];
        if (method != null) {
            Map<String, IOpenClass> m = new HashMap<String, IOpenClass>();
            String[] typeNames = new String[method.getParameterTypes().length];
            int[] arrayDims = new int[method.getParameterTypes().length];
            int i = 0;
            for (Type type : method.getJavaMethod().getGenericParameterTypes()) {
                typeNames[i] = JavaGenericsUtils.getGenericTypeName(type);
                if (typeNames[i] != null) {
                    arrayDims[i] = JavaGenericsUtils.getGenericTypeDim(type);
                    int arrayDim = arrayDims[i];
                    IOpenClass t = callParam[i];
                    if (t.getInstanceClass() != null) {
                        t = JavaOpenClass.getOpenClass(t.getInstanceClass()); // don't
                                                                              // use
                                                                              // alias
                                                                              // datatypes
                                                                              // as
                                                                              // Generics
                    }

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
                    if (t.isArray()) {
                        t = JavaOpenClass.OBJECT;
                    }
                    if (m.containsKey(typeNames[i])) {
                        IOpenClass existedType = m.get(typeNames[i]);
                        IOpenCast cast1 = casts.getCast(existedType, t);
                        IOpenCast cast2 = casts.getCast(t, existedType);
                        if ((cast1 == null || !cast1.isImplicit()) && (cast2 == null || !cast2.isImplicit())) {
                            IOpenClass clazz = casts.findClosestClass(t, existedType);
                            if (clazz != null) {
                                m.put(typeNames[i], unwrapPrimitiveClassIfNeeded(clazz));
                            } else {
                                return NO_MATCH;
                            }
                        } else if ((cast1 == null || !cast1.isImplicit())) {
                        } else if ((cast2 == null || !cast2.isImplicit())) {
                            m.put(typeNames[i], t);
                        } else {
                            if (cast2.getDistance() < cast1.getDistance()) {
                                m.put(typeNames[i], t);
                            }
                        }
                    } else {
                        m.put(typeNames[i], unwrapPrimitiveClassIfNeeded(t));
                    }
                }
                i++;
            }

            String returnType = JavaGenericsUtils.getGenericTypeName(method.getJavaMethod().getGenericReturnType());

            if (returnType != null && m.containsKey(returnType)) {
                int dim = JavaGenericsUtils.getGenericTypeDim(method.getJavaMethod().getGenericReturnType());
                IOpenClass type = m.get(returnType);
                if (dim > 0) {
                    type = type.getArrayType(dim);
                }
                IOpenCast returnCast = casts.getCast(method.getType(), type);
                if (returnCast == null) {
                    return NO_MATCH;
                }
                returnCastHolder[0] = returnCast;
                returnTypeHolder[0] = type;
            }

            for (i = 0; i < callParam.length; i++) {
                if (typeNames[i] != null) {
                    IOpenClass type = m.get(typeNames[i]);
                    if (arrayDims[i] > 0) {
                        type = type.getArrayType(arrayDims[i]);
                    }
                    if (callParam[i] != type) {
                        IOpenCast gCast = casts.getCast(callParam[i], type);
                        // params[i] = type;
                        if (type != methodParam[i]) {
                            IOpenCast cast = casts.getCast(type, methodParam[i]);
                            if (cast == null || !cast.isImplicit()) {
                                return NO_MATCH;
                            }
                            castHolder[i] = new CastsLinkageCast(gCast, cast);
                        } else {
                            castHolder[i] = gCast;
                        }
                        castHolderDistance[i] = gCast.getDistance();
                    } else {
                        if (callParam[i] != methodParam[i]) {
                            castHolder[i] = casts.getCast(callParam[i], methodParam[i]);
                            if (castHolder[i] == null || !castHolder[i].isImplicit()) {
                                return NO_MATCH;
                            }
                        }
                    }
                } else {
                    if (callParam[i] != methodParam[i]) {
                        castHolder[i] = casts.getCast(callParam[i], methodParam[i]);
                        if (castHolder[i] == null || !castHolder[i].isImplicit()) {
                            return NO_MATCH;
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < callParam.length; i++) {
                if (callParam[i] != methodParam[i]) {
                    IOpenCast cast = casts.getCast(callParam[i], methodParam[i]);
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
        
        for (int i = 0;i<castHolder.length;i++) {
            if (castHolder[i] instanceof IgnoredByMethodSearchOpenCast) {
                return NO_MATCH;
            }
        }
        
        return m;
    }

    private static IOpenClass unwrapPrimitiveClassIfNeeded(IOpenClass clazz) {
        if (clazz != null && clazz.getInstanceClass() != null && clazz.getInstanceClass()
            .isPrimitive()) {
            return JavaOpenClass
                .getOpenClass(ClassUtils.primitiveToWrapper(clazz.getInstanceClass()));
        }
        return clazz;
    }

    private static final boolean zeroCasts(int[] m) {
        for (int i = 0; i < m.length; i++) {
            if (m[i] != 0) {
                return false;
            }
        }
        return true;
    }

    private static final boolean lq(int[] m1, int[] m2) {
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

    private static final boolean eq(int[] distances1, int[] distances2) {
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
            ICastFactory casts,
            Iterable<IOpenMethod> methods) throws AmbiguousMethodException {

        final int nParams = params.length;
        Iterable<IOpenMethod> filtered = (methods == null) ? Collections
            .<IOpenMethod> emptyList() : CollectionUtils.findAll(methods, new CollectionUtils.Predicate<IOpenMethod>() {
                @Override
                public boolean evaluate(IOpenMethod method) {
                    return method.getName().equals(name) && method.getSignature().getParameterTypes().length == nParams;
                }
            });

        List<IOpenMethod> matchingMethods = new ArrayList<IOpenMethod>();
        List<IOpenCast[]> matchingMethodsCastHolder = new ArrayList<IOpenCast[]>();
        List<IOpenCast> matchingMethodsReturnCast = new ArrayList<IOpenCast>();
        List<IOpenClass> matchingMethodsReturnType = new ArrayList<IOpenClass>();
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
                    casts,
                    castHolder,
                    returnCastHolder,
                    returnTypeHolder);
            } else {
                match = calcMatch(null,
                    method.getSignature().getParameterTypes(),
                    params,
                    casts,
                    castHolder,
                    returnCastHolder,
                    returnTypeHolder);
            }
            if (match == NO_MATCH) {
                continue;
            }
            if (lq(match, bestMatch)) {
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
                if (!(zeroCasts(bestMatch))) {
                    CastingMethodCaller methodCaller = new CastingMethodCaller(m, matchingMethodsCastHolder.get(0));
                    return buildMethod(matchingMethodsReturnCast.get(0),
                        matchingMethodsReturnType.get(0),
                        m,
                        methodCaller);
                } else {
                    return buildMethod(matchingMethodsReturnCast.get(0), matchingMethodsReturnType.get(0), m, m);
                }
            default:
                IOpenMethod mostSecificMethod = findMostSpecificMethod(name, params, matchingMethods, casts);
                boolean f = true;
                for (int i = 0; i < nParams; i++) {
                    if (!params[i].equals(mostSecificMethod.getSignature().getParameterType(i))) {
                        f = false;
                        break;
                    }
                }
                if (f) {
                    return mostSecificMethod;
                } else {
                    int k = 0;
                    for (int i = 0; i < matchingMethods.size(); i++) {
                        if (matchingMethods.get(i) == mostSecificMethod) {
                            k = i;
                            break;
                        }
                    }
                    CastingMethodCaller methodCaller = new CastingMethodCaller(mostSecificMethod,
                        matchingMethodsCastHolder.get(k));
                    IOpenCast c = matchingMethodsReturnCast.get(k);
                    IOpenClass t = matchingMethodsReturnType.get(k);
                    if (c != null && t != mostSecificMethod.getType()) {
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
        if (methodsReturnCast != null && methodsReturnType != m.getType()) {
            return new AutoCastableResultOpenMethod(methodCaller, methodsReturnType, methodsReturnCast);
        } else {
            return methodCaller;
        }
    }

    private static IMethodCaller findVarArgMethod(final String name,
            IOpenClass[] params,
            ICastFactory casts,
            Iterable<IOpenMethod> methods) throws AmbiguousMethodException {
        Iterable<IOpenMethod> filtered = (methods == null) ? Collections
            .<IOpenMethod> emptyList() : CollectionUtils.findAll(methods, new CollectionUtils.Predicate<IOpenMethod>() {
                @Override
                public boolean evaluate(IOpenMethod method) {
                    return method.getName().equals(name) && method.getSignature().getNumberOfParameters() > 0 && method
                        .getSignature()
                        .getParameterType(method.getSignature().getNumberOfParameters() - 1)
                        .isArray();
                }
            });
        if (filtered.iterator().hasNext()) {
            for (int i = params.length - 1; i >= 0; i--) {
                IOpenClass[] args = new IOpenClass[i + 1];
                System.arraycopy(params, 0, args, 0, i);
                IOpenClass varArgType = params[i];
                for (int j = i + 1; j < params.length; j++) {
                    varArgType = OpenClassUtils.findParentClassWithBoxing(varArgType, params[j]);
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
                    args[i] = varArgType.getAggregateInfo().getIndexedAggregateType(varArgType, 1);
                }

                IMethodCaller matchedMethod = findCastingMethod(name, args, casts, filtered);
                if (matchedMethod != null) {
                    if (NullOpenClass.isAnyNull(varArgType)) {
                        int lastParameterIndex = matchedMethod.getMethod().getSignature().getNumberOfParameters() - 1;
                        return new VarArgsOpenMethod(matchedMethod, matchedMethod.getMethod().getSignature().getParameterType(lastParameterIndex).getComponentClass().getInstanceClass(), i);
                    } else {
                        return new VarArgsOpenMethod(matchedMethod, varArgType.getInstanceClass(), i);
                    }
                }
            }
            for (int i = params.length - 1; i >= 0; i--) {
                IOpenClass[] args = new IOpenClass[i + 1];
                System.arraycopy(params, 0, args, 0, i);
                IOpenClass varArgType = params[i];
                for (int j = i + 1; j < params.length; j++) {
                    varArgType = casts.findClosestClass(varArgType, params[j]);
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
                    args[i] = varArgType.getAggregateInfo().getIndexedAggregateType(varArgType, 1);
                }
                args[i] = varArgType.getAggregateInfo().getIndexedAggregateType(varArgType, 1);

                IMethodCaller matchedMethod = findCastingMethod(name, args, casts, filtered);
                if (matchedMethod != null) {
                    IOpenCast[] parameterCasts = new IOpenCast[params.length - i];
                    for (int j = 0; j < params.length - i; j++) {
                        parameterCasts[j] = casts.getCast(params[i + j], varArgType);
                    }
                    if (NullOpenClass.isAnyNull(varArgType)) {
                        int lastParameterIndex = matchedMethod.getMethod().getSignature().getNumberOfParameters() - 1;
                        return new VarArgsOpenMethod(matchedMethod, matchedMethod.getMethod().getSignature().getParameterType(lastParameterIndex).getComponentClass().getInstanceClass(), i, parameterCasts);
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
     * @see <a href=
     *      "http://java.sun.com/docs/books/jls/second_edition/html/expressions.doc.html#18428"
     *      >java documentation </a >
     * 
     * 
     * @param name The name of the method.
     * @param params Argument types of the method.
     * @param matchingMethods All matching methods for this argument types.
     * @param casts OpenL cast factory.
     * 
     * @return The most specific method from matching methods collection.
     * 
     * @throws AmbiguousMethodException Exception will be thrown if most
     *             specific method can not be determined.
     */
    private static IOpenMethod findMostSpecificMethod(String name,
            IOpenClass[] params,
            List<IOpenMethod> matchingMethods,
            ICastFactory casts) throws AmbiguousMethodException {
        List<IOpenMethod> moreSpecificMethods = new ArrayList<IOpenMethod>();
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
            List<IOpenMethod> mostSpecificMethods = new ArrayList<IOpenMethod>();
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
            if (!firstDeclaringClass.equals(secondDeclaringClass)) {
                if (secondDeclaringClass.isAssignableFrom(firstDeclaringClass)) {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.binding.IMethodFactory#getMethod(java.lang.String,
     * org.openl.types.IOpenClass[], org.openl.binding.ICastFactory)
     */
    public static IMethodCaller findMethod(String name,
            IOpenClass[] params,
            ICastFactory casts,
            IMethodFactory factory) throws AmbiguousMethodException {
        return findMethod(name, params, casts, factory, false);
    }

    public static IMethodCaller findConstructor(IOpenClass[] params,
                                                ICastFactory casts,
                                                IMethodFactory factory) throws AmbiguousMethodException {
        IMethodCaller caller = factory.getConstructor(params);
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
            ICastFactory casts,
            IMethodFactory factory,
            boolean strictMatch) throws AmbiguousMethodException {
        IMethodCaller caller = factory.getMethod(name, params);
        if (caller != null) {
            return caller;
        }
        if (params.length == 0 || casts == null) {
            return null;
        }
        if (!strictMatch) {
            return findMethod(name, params, casts, factory.methods(name));
        }
        return null;
    }

    public static IMethodCaller findMethod(String name,
            IOpenClass[] params,
            ICastFactory casts,
            Iterable<IOpenMethod> methods) throws AmbiguousMethodException {
        IMethodCaller caller = findCastingMethod(name, params, casts, methods);
        if (caller != null) {
            return caller;
        }
        return findVarArgMethod(name, params, casts, methods);
    }
}
