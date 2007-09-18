package com.exigen.openl.component;

import java.util.List;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.types.IOpenClass;

import com.exigen.common.component.ComponentBuilder;
import com.exigen.common.component.ComponentInstantiationException;
import com.exigen.common.component.ComponentReference;
import com.exigen.common.component.ParameterValue;
import com.exigen.common.component.java.AbstractJava;
import com.exigen.common.component.java.HolderComponentReference;
import com.exigen.common.model.components.ComponentDefinition;
import com.exigen.common.util.runtime.Assert;
import com.exigen.openl.model.openl.RuleSetFile;

public class RuleComponentBuilder extends AbstractJava implements ComponentBuilder {
	public static String OPENL_NAME = "org.openl.xls";
	public static String USER_HOME = ".";


	public ComponentReference createComponent(ComponentDefinition componentDefinition,
			List<ParameterValue> params,
			Map<String, Map<String, Object>> context) throws ComponentInstantiationException {
		
		Assert.isTrue(componentDefinition instanceof RuleSetFile);
		RuleSetFile ruleSetFile = (RuleSetFile) componentDefinition;
		String fileName = ExcelUtil.getFileName( ruleSetFile);
		ClassLoader classLoader = getClassLoader(context);
		
		
		
		OpenClassJavaWrapper wrapper = loadClass(classLoader,fileName);
		IOpenClass clazz = wrapper.getOpenClassWithErrors();
		Object instance = clazz.newInstance(wrapper.getEnv());
		OpenInstance openInstance = new OpenInstance(clazz,instance,wrapper.getEnv());
		return new HolderComponentReference(openInstance);
	}
	public OpenClassJavaWrapper loadClass(ClassLoader classLoader, String fileName){
		UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), USER_HOME);

		OpenL openl = OpenL.getInstance(OPENL_NAME, ucxt);

		
		ResourceSourceCodeModule srcModule = new ResourceSourceCodeModule(
				classLoader,
				fileName, null);
		
		
		CompiledOpenClass openClass = openl.compileModuleWithErrors(srcModule);
		OpenClassJavaWrapper wrapper = new OpenClassJavaWrapper(openClass,
				openl.getVm().getRuntimeEnv());
		return wrapper;
	}

}
