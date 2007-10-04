package com.exigen.openl.component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ClassUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

import com.exigen.common.component.Executor;
import com.exigen.common.component.OperationExecutionException;
import com.exigen.common.component.ParameterValue;
import com.exigen.common.component.java.AbstractJava;
import com.exigen.common.component.java.MethodExecutionInfo;
import com.exigen.common.model.components.OperationDefinition;
import com.exigen.common.util.runtime.Assert;
import com.exigen.openl.model.openl.RuleSet;

public class RuleSetExecutor extends AbstractJava implements Executor {

	public void execute(OperationDefinition operationDefinition,
			Object componentInstance, List<ParameterValue> input,
			List<ParameterValue> output,
			Map<String, Map<String, Object>> context)
			throws OperationExecutionException {
		Assert.isTrue(operationDefinition instanceof RuleSet);
		RuleSet ruleSet = (RuleSet) operationDefinition;

		String methodName = ruleSet.getName();
		Assert.isNotNull(methodName);

		Assert.isNotNull(componentInstance);

		Assert.isTrue(componentInstance instanceof OpenInstance);

		OpenInstance openInstance = (OpenInstance) componentInstance;

		ClassLoader classLoader = openInstance.getClassLoader();
		
		MethodExecutionInfo info = new MethodExecutionInfo(ruleSet,input,output,classLoader);
		info.intialize();

		
		IOpenMethod method = null;
		nextMethod: for (Iterator methodIter = openInstance.getOpenClass()
				.methods(); methodIter.hasNext();) {
			IOpenMethod testMethod = (IOpenMethod) methodIter.next();

			if (!methodName.equals(testMethod.getName()))
				continue nextMethod;
			if (testMethod.getSignature().getNumberOfArguments() != info.paramClasses.length)
				continue nextMethod;

			int index = 0;
			for (IOpenClass paramType : testMethod.getSignature()
					.getParameterTypes()) {
				Class<?> openlClass = paramType.getOpenClass()
						.getInstanceClass();
				if (openlClass.isPrimitive()) {
					openlClass = ClassUtils.primitiveToWrapper(paramType
							.getOpenClass().getInstanceClass());
				}
				if (!openlClass.isAssignableFrom(info.paramClasses[index++]))
					continue nextMethod;
			}
			method = testMethod;
			break;
		}

		if (method == null)
			throw new OperationExecutionException("cannot find method \""
					+ methodName + "\"");

		Object result = method.invoke(openInstance.getInstance(), info.paramValues,
				openInstance.getEnv());
		info.saveResult(result);
	}

}
