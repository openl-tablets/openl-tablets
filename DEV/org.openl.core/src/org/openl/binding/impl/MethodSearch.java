/*
 * Created on Jun 17, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.binding.ICastFactory;
import org.openl.binding.IMethodFactory;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.impl.cast.CastsLinkageCast;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.CastingMethodCaller;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.AutoCastResultOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenMethod;
import org.openl.util.CollectionUtils;
import org.openl.util.GenericUtils;

/**
 * @author snshor
 *
 */
public class MethodSearch {

	static final int NO_MATCH = Integer.MAX_VALUE;

	protected static int calcMatch(JavaOpenMethod method, IOpenClass[] methodParam, IOpenClass[] callParam,
			ICastFactory casts, IOpenCast[] castHolder, IOpenCast[] returnCastHolder, IOpenClass[] returnTypeHolder) {
		int maxdiff = 0;
		int ndiff = 0;

		if (method != null) {
			Map<String, IOpenClass> m = new HashMap<String, IOpenClass>();
			String[] typeNames = new String[method.getParameterTypes().length];
			int[] arrayDims = new int[method.getParameterTypes().length];
			int i = 0;
			for (Type type : method.getJavaMethod().getGenericParameterTypes()) {
				typeNames[i] = GenericUtils.getGenericTypeName(type);
				if (typeNames[i] != null) {
					arrayDims[i] = GenericUtils.getGenericTypeDim(type);
					int arrayDim = arrayDims[i];
					IOpenClass t = callParam[i];
					while (t.isArray() && arrayDim > 0) {
						arrayDim--;
						t = t.getComponentClass();
					}
					if (arrayDim > 0) {
						return NO_MATCH;
					}
					if (t.isArray()){
						t = JavaOpenClass.OBJECT;
					}
					if (m.containsKey(typeNames[i])) {
						IOpenClass existedType = m.get(typeNames[i]);
						IOpenCast cast = casts.getCast(existedType, t);
						if (cast == null || !cast.isImplicit()) {
							cast = casts.getCast(t, existedType);
							if (cast == null || !cast.isImplicit()) {
								return NO_MATCH;
							}
						} else {
							m.put(typeNames[i], t);
						}
					} else {
						m.put(typeNames[i], t);
					}
				}
				i++;
			}
			
			String returnType = GenericUtils.getGenericTypeName(method.getJavaMethod().getGenericReturnType());
			
			if (returnType != null && m.containsKey(returnType)){
				int dim = GenericUtils.getGenericTypeDim(method.getJavaMethod().getGenericReturnType());
				IOpenClass type = buildTypeWithArrayLogic(m.get(returnType), dim);
				IOpenCast returnCast = casts.getCast(method.getType(), type);
				if (returnCast == null) {
					return NO_MATCH;
				}
				returnCastHolder[0] = returnCast;
				returnTypeHolder[0] = type;
			}
			
			for (i = 0; i < callParam.length; i++) {
				if (typeNames[i] != null) {
					IOpenClass type = buildTypeWithArrayLogic(m.get(typeNames[i]), arrayDims[i]);
					
					if (callParam[i] != type) {
						IOpenCast gCast = casts.getCast(callParam[i], type);
						if (type != methodParam[i]) {
							IOpenCast cast = casts.getCast(type, methodParam[i]);
							if (cast == null || !cast.isImplicit()) {
								return NO_MATCH;
							}
							castHolder[i] = new CastsLinkageCast(gCast, cast);
						} else {
							castHolder[i] = gCast;
						}
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

		for (int i = 0; i < callParam.length; i++) {
			if (castHolder[i] != null) {
				maxdiff = Math.max(maxdiff, castHolder[i].getDistance(callParam[i], methodParam[i]));
				ndiff++;
			}
		}

		return maxdiff * 100 + ndiff;
	}

	private static IOpenClass buildTypeWithArrayLogic(IOpenClass type, int dim) {
		if (dim > 0){
			IOpenClass arrayType = JavaOpenClass
					.getOpenClass(Array.newInstance(type.getInstanceClass(), dim).getClass());
			if (type.getDomain() != null) {
				StringBuilder domainOpenClassName = new StringBuilder(type.getName());
				for (int j = 0; j < dim; j++) {
					domainOpenClassName.append("[]");
				}
				DomainOpenClass domainArrayType = new DomainOpenClass(domainOpenClassName.toString(), arrayType,
						type.getDomain(), null);
				return domainArrayType;
			} else {
				return arrayType;
			}
		}
		return type;
	}

	public static IMethodCaller getCastingMethodCaller(final String name, IOpenClass[] params, ICastFactory casts,
			Iterable<IOpenMethod> methods) throws AmbiguousMethodException {

		List<IOpenMethod> matchingMethods = new ArrayList<IOpenMethod>();
		List<IOpenCast[]> matchingMethodsCastHolder = new ArrayList<IOpenCast[]>();
		List<IOpenCast> matchingMethodsReturnCast = new ArrayList<IOpenCast>();
		List<IOpenClass> matchingMethodsReturnType = new ArrayList<IOpenClass>();
		int bestMatch = NO_MATCH;

		final int nParams = params.length;
		Iterable<IOpenMethod> filtered = (methods == null) ? Collections.<IOpenMethod>emptyList()
				: CollectionUtils.findAll(methods, new CollectionUtils.Predicate<IOpenMethod>() {
					@Override
					public boolean evaluate(IOpenMethod method) {
						return method.getName().equals(name)
								&& method.getSignature().getParameterTypes().length == nParams;
					}
				});
		for (IOpenMethod method : filtered) {
			IOpenCast[] castHolder = new IOpenCast[nParams];
			IOpenCast[] returnCastHolder = new IOpenCast[1];
			IOpenClass[] returnTypeHolder = new IOpenClass[1];
			int match;
			if (method instanceof JavaOpenMethod) { //Process Java Generics
				JavaOpenMethod javaOpenMethod = (JavaOpenMethod) method;
				match = calcMatch(javaOpenMethod, method.getSignature().getParameterTypes(), params,
						casts, castHolder, returnCastHolder, returnTypeHolder);
			} else {
				match = calcMatch(null, method.getSignature().getParameterTypes(), params, casts, castHolder, returnCastHolder, returnTypeHolder);
			}
			if (match == NO_MATCH) {
				continue;
			}
			if (match < bestMatch) {
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

			if (match == bestMatch) {
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
			if (bestMatch > 0) {
				IOpenMethod m = matchingMethods.get(0);
				CastingMethodCaller methodCaller = new CastingMethodCaller(m, matchingMethodsCastHolder.get(0));
				IOpenCast c = matchingMethodsReturnCast.get(0);
				IOpenClass t = matchingMethodsReturnType.get(0);
				if (c != null && t != m.getType()){
					return new AutoCastResultOpenMethod(methodCaller, t, c);
				}else{
					return methodCaller;
				}
			} else {
				return matchingMethods.get(0);
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
				CastingMethodCaller methodCaller = new CastingMethodCaller(mostSecificMethod, matchingMethodsCastHolder.get(k));
				IOpenCast c = matchingMethodsReturnCast.get(k);
				IOpenClass t = matchingMethodsReturnType.get(k);
				if (c != null && t != mostSecificMethod.getType()) {
					return new AutoCastResultOpenMethod(methodCaller, t, c);
				}else{
					return methodCaller;
				}
			}
		}
	}

	public static IMethodCaller getCastingMethodCaller(String name, IOpenClass[] params, ICastFactory casts,
			IMethodFactory factory) throws AmbiguousMethodException {
		return getCastingMethodCaller(name, params, casts, factory.methods(name));
	}

	public static IMethodCaller getCastingConstructorCaller(String name, IOpenClass[] params, ICastFactory casts,
			IMethodFactory factory) throws AmbiguousMethodException {
		return getCastingMethodCaller(name, params, casts, factory.constructors(name));
	}

	/**
	 * Choosing the most specific method according to:
	 * 
	 * @see <a href=
	 *      "http://java.sun.com/docs/books/jls/second_edition/html/expressions.doc.html#18428"
	 *      >java documentation </a >
	 * 
	 * 
	 * @param name
	 *            The name of the method.
	 * @param params
	 *            Argument types of the method.
	 * @param matchingMethods
	 *            All matching methods for this argument types.
	 * @param casts
	 *            OpenL cast factory.
	 * 
	 * @return The most specific method from matching methods collection.
	 * 
	 * @throws AmbiguousMethodException
	 *             Exception will be thrown if most specific method can not be
	 *             determined.
	 */
	private static IOpenMethod findMostSpecificMethod(String name, IOpenClass[] params,
			List<IOpenMethod> matchingMethods, ICastFactory casts) throws AmbiguousMethodException {
		for (IOpenMethod res : matchingMethods) {
			boolean f = true;
			for (IOpenMethod next : matchingMethods) {
				if (res != next && !isMoreSpecificMethod(res, next, casts)) {
					f = false;
					break;
				}
			}
			if (f) {
				return res;
			}
		}

		throw new AmbiguousMethodException(name, params, matchingMethods);
	}

	private static boolean isMoreSpecificMethod(IOpenMethod first, IOpenMethod second, ICastFactory casts) {
		if (first.getSignature().getNumberOfParameters() != second.getSignature().getNumberOfParameters()) {
			return false;
		}
		boolean differenceInArgTypes = false;
		// more specific arg types
		for (int i = 0; i < first.getSignature().getNumberOfParameters(); i++) {
			IOpenClass firstArgType = first.getSignature().getParameterType(i);
			IOpenClass secondArgType = second.getSignature().getParameterType(i);
			if (!firstArgType.equals(secondArgType)) {
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
	public static IMethodCaller getMethodCaller(String name, IOpenClass[] params, ICastFactory casts,
			IMethodFactory factory) throws AmbiguousMethodException {
		return getMethodCaller(name, params, casts, factory, false);
	}

	public static IMethodCaller getConstructorCaller(String name, IOpenClass[] params, ICastFactory casts,
			IMethodFactory factory) throws AmbiguousMethodException {
		return getConstructorCaller(name, params, casts, factory, false);
	}

	public static IMethodCaller getMethodCaller(String name, IOpenClass[] params, ICastFactory casts,
			IMethodFactory factory, boolean strictMatch) throws AmbiguousMethodException {
		IMethodCaller caller = factory.getMatchingMethod(name, params);
		if (caller != null) {
			return caller;
		}

		if (params.length == 0 || casts == null) {
			return null;
		}
		if (!strictMatch) {
			return getCastingMethodCaller(name, params, casts, factory);
		}
		return null;
	}

	public static IMethodCaller getConstructorCaller(String name, IOpenClass[] params, ICastFactory casts,
			IMethodFactory factory, boolean strictMatch) throws AmbiguousMethodException {
		IMethodCaller caller = factory.getMatchingConstructor(name, params);
		if (caller != null) {
			return caller;
		}

		if (params.length == 0 || casts == null) {
			return null;
		}
		if (!strictMatch) {
			return getCastingConstructorCaller(name, params, casts, factory);
		}
		return null;
	}
}
