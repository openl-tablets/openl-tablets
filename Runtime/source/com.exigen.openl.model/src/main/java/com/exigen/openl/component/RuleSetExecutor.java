package com.exigen.openl.component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

import com.exigen.common.component.Executor;
import com.exigen.common.component.OperationExecutionException;
import com.exigen.common.component.ParameterValue;
import com.exigen.common.component.java.AbstractJava;
import com.exigen.common.component.util.java.JavaException;
import com.exigen.common.component.util.java.JavaUtils;
import com.exigen.common.model.components.OperationDefinition;
import com.exigen.common.model.components.java.JavaMethodParameter;
import com.exigen.common.model.components.java.JavaMethodReturn;
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

		ClassLoader classLoader = getClassLoader(context);
		
		Class[] paramClasses = new Class[ruleSet.getMethodParameters().size()];
		Object[] paramValues = new Object[ruleSet.getMethodParameters().size()];
		Class<?> returnClass = null;
		int i = 0;
		for (Iterator iter = ruleSet.getMethodParameters().iterator(); iter
				.hasNext();) {
			JavaMethodParameter param = (JavaMethodParameter) iter.next();
			Assert.isNotNull(param.getType());
			try {
				paramClasses[i] = JavaUtils.loadClass(param.getType(),
						classLoader);
			} catch (JavaException e) {
				throw new OperationExecutionException(
						"Unable to load parameter class " + param.getType(), e);
			}
			paramValues[i] = input.get(i).getValue();
			i++;
		}
		if (ruleSet.getReturn() != null) {
			JavaMethodReturn param = ruleSet.getReturn();
			try {
				Assert.isNotNull(param.getType());
				returnClass = JavaUtils.loadClass(param.getType(), classLoader);
			} catch (JavaException e) {
				throw new OperationExecutionException(
						"Unable to load return class " + param.getType(), e);
			}

		}

		IOpenClass[] openParamClasses  = new IOpenClass[paramClasses.length];
		for (int j = 0; j < openParamClasses.length; j++) {
			openParamClasses[j] =  JavaOpenClass.getOpenClass( paramClasses[j]);
		}

		IOpenMethod method = openInstance.getOpenClass().getMatchingMethod(methodName, openParamClasses);
		Object result = method.invoke(openInstance.getInstance(), paramValues, openInstance.getEnv());
		if (result != null && returnClass != null) {
			Assert.isTrue(returnClass.isAssignableFrom(result.getClass()));
		}
		
		if (output.size() > 0) {
			output.get(0).setValue(result);
		}
	}

}
