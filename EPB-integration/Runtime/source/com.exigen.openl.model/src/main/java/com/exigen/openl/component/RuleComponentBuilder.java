package com.exigen.openl.component;

import java.util.HashMap;
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
import com.exigen.common.component.util.ComponentContextHelper;
import com.exigen.common.model.components.ComponentDefinition;
import com.exigen.common.repository.MFileManager;
import com.exigen.common.repository.classloader.ClassLoaderUtil;
import com.exigen.common.util.runtime.Assert;
import com.exigen.openl.model.openl.RuleSetFile;

public class RuleComponentBuilder extends AbstractJava implements ComponentBuilder {
	public static String OPENL_NAME = "org.openl.xls";
	public static String USER_HOME = ".";

	class Key {
		MFileManager fileManager;
		String fileName;
		public Key(MFileManager fileManager, String fileName) {
			super();
			this.fileManager = fileManager;
			this.fileName = fileName;
		}
		@Override
		public int hashCode() {
			final int PRIME = 31;
			int result = 1;
			result = PRIME * result + ((fileManager == null) ? 0 : fileManager.hashCode());
			result = PRIME * result + ((fileName == null) ? 0 : fileName.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final Key other = (Key) obj;
			if (fileManager == null) {
				if (other.fileManager != null)
					return false;
			} else if (!fileManager.equals(other.fileManager))
				return false;
			if (fileName == null) {
				if (other.fileName != null)
					return false;
			} else if (!fileName.equals(other.fileName))
				return false;
			return true;
		}
		
		
	}
	
	private static final Map<Key, OpenClassJavaWrapper> cache = new HashMap<Key, OpenClassJavaWrapper>();

	public ComponentReference createComponent(
			ComponentDefinition componentDefinition,
			List<ParameterValue> params,
			Map<String, Map<String, Object>> context)
			throws ComponentInstantiationException {

		Assert.isTrue(componentDefinition instanceof RuleSetFile);
		RuleSetFile ruleSetFile = (RuleSetFile) componentDefinition;
		String fileName = ExcelUtil.getFileName(ruleSetFile);
		// ClassLoader classLoader = getClassLoader(context);

		MFileManager fileManager = ComponentContextHelper
				.getFileManager(context);
		ClassLoader classLoader = ClassLoaderUtil
				.getFileManagerClassLoader(fileManager);
		Key key = new Key(fileManager, fileName);
		OpenClassJavaWrapper wrapper = cache.get(key);
		if (wrapper == null) {
			fileManager.loadAll();
			wrapper = loadClass(classLoader, fileName);
			cache.put(key, wrapper);
		}
		IOpenClass clazz = wrapper.getOpenClassWithErrors();
		Object instance = clazz.newInstance(wrapper.getEnv());
		OpenInstance openInstance = new OpenInstance(classLoader, clazz,
				instance, wrapper.getEnv());
		return new HolderComponentReference(openInstance);
	}

	public OpenClassJavaWrapper loadClass(ClassLoader classLoader, String fileName){
		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(classLoader);
		try {
			UserContext ucxt = new UserContext(classLoader, USER_HOME);
	
			OpenL openl = OpenL.getInstance(OPENL_NAME, ucxt);
	
			
			ResourceSourceCodeModule srcModule = new ResourceSourceCodeModule(
					classLoader,
					fileName, null);
			
			
			CompiledOpenClass openClass = openl.compileModuleWithErrors(srcModule);
			OpenClassJavaWrapper wrapper = new OpenClassJavaWrapper(openClass,
					openl.getVm().getRuntimeEnv());

			return wrapper;
		} finally {
			Thread.currentThread().setContextClassLoader(currentClassLoader);
		}
	}

}
