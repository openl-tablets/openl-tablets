package org.openl.binding.impl.ce;

import java.lang.reflect.Array;

import org.openl.binding.IBoundNode;
import org.openl.binding.impl.MethodBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.IConvertor;
import org.openl.util.ce.IMTConvertorFactory;
import org.openl.util.ce.conf.ServiceMTConfiguration;
import org.openl.util.ce.impl.ServiceMT;
import org.openl.vm.IRuntimeEnv;

/**
 * Bound node for methods such as
 * <code>'double[] calculate(Premium[] premiumObj)'</code>. Is based on the
 * method with signature <code>'double calculate(Premium premiumObj)'</code> by
 * evaluating it several times on runtime.
 * 
 * @author DLiauchuk
 * 
 *         Updated for multi-threaded usage
 * 
 * @author snshor
 * 
 */
public class MultiCallMethodBoundNodeMT extends MethodBoundNode {

	private IOpenClass returnType;

	/** the index of the argument in the method signature that is an array **/
	private int arrayArgumentIndex;

	/**
	 * 
	 * @param syntaxNode
	 *            will be represents like <code>'calculate(parameter)'</code>
	 * @param children
	 *            its gonna be only one children, that represents the parameter
	 *            in method call.
	 * @param singleParameterMethod
	 *            method for single(not array) parameter in signature
	 * @param arrayArgumentIndex
	 *            the index of the argument in method signature that is an array
	 */
	public MultiCallMethodBoundNodeMT(ISyntaxNode syntaxNode,
			IBoundNode[] children, IMethodCaller singleParameterMethod,
			int arrayArgumentIndex) {
		super(syntaxNode, children, singleParameterMethod);
		this.arrayArgumentIndex = arrayArgumentIndex;
	}

	public Object evaluateRuntime(final IRuntimeEnv env)
			throws OpenLRuntimeException {
		final Object target = getTargetNode() == null ? env.getThis()
				: getTargetNode().evaluate(env);
		final Object[] methodParameters = evaluateChildren(env);

		// gets the values of array parameters
		final Object arrayParameters = methodParameters[arrayArgumentIndex];

		int paramsLength = 0;
		if (arrayParameters != null) {
			paramsLength = Array.getLength(arrayParameters);
		}

		Object results = null;

		if (JavaOpenClass.VOID.equals(super.getType())) {
			// for void type return the last return value, as it doens`t matter,
			// it will be null,
			// for all the calls
			//
			for (int callIndex = 0; callIndex < paramsLength; callIndex++) {
				results = getMethodCaller().invoke(
						target,
						initParametersForSingleCall(methodParameters,
								arrayParameters, callIndex), env);
			}
		} else {

			Class<?> type = super.getType().getInstanceClass();
			// create an array of results
			//
			results = Array.newInstance(type, paramsLength);

			// populate the results array by invoking method for single
			// parameter
			//

			final IMethodCaller mcaller = getMethodCaller();
			ServiceMTConfiguration config = ServiceMT.getService().getConfig();
			long singleMethodLengthConf = config.getComponentLengthNs(mcaller.getMethod().getName());
			
			final long singleMethodLength = singleMethodLengthConf == 0 ? config.getDefaultRunningComponentLength() : singleMethodLengthConf;
			

			IMTConvertorFactory<Object> factory = new IMTConvertorFactory<Object>() {

				@Override
				public IConvertor<Integer, Object> makeConvertorInstance() {
					
					
					IConvertor<Integer, Object> conv = new IConvertor<Integer, Object>() {

						final IRuntimeEnv myenv = env.cloneEnvForMT();

						@Override
						public Object convert(final Integer index) {
							return mcaller.invoke(
									target,
									initParametersForSingleCall(
											methodParameters, arrayParameters,
											index), myenv);
						}

					};
					
					return conv;
				}

				@Override
				public long estimateDuration(Integer index) {
					return singleMethodLength;
				}

			};

			if (!type.isPrimitive())
				ServiceMT.getService().executeIndexed(factory , (Object[]) results);
			else
				ServiceMT.getService().executeIndexedPrimitive(null, results,
						singleMethodLength);

		}

		return results;
	}

	private Object[] initParametersForSingleCall(Object[] allParameters,
			Object arrayParameters, int callIndex) {
		// create an array of parameters that will be used for current call
		//
		Object[] callParameters = allParameters.clone();
		Array.set(callParameters, arrayArgumentIndex,
				Array.get(arrayParameters, callIndex));

		return callParameters;
	}

	public IOpenClass getType() {
		if (returnType == null) {
			// gets the return type of bound node, it will be the single type.
			//
			IOpenClass singleReturnType = super.getType();

			// create an array type.
			//
			returnType = singleReturnType.getAggregateInfo()
					.getIndexedAggregateType(singleReturnType, 1);
		}
		return returnType;
	}
}
