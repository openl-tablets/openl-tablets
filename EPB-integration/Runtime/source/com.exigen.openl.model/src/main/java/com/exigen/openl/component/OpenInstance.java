package com.exigen.openl.component;

import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class OpenInstance {
	ClassLoader classLoader;
	IOpenClass openClass;
	Object instance;
	IRuntimeEnv env;
	public OpenInstance(ClassLoader classLoader, IOpenClass openClass, Object instance, IRuntimeEnv env) {
		super();
		this.classLoader = classLoader;
		this.openClass = openClass;
		this.instance = instance;
		this.env = env;
	}
	public ClassLoader getClassLoader() {
		return classLoader;
	}
	public Object getInstance() {
		return instance;
	}
	public IOpenClass getOpenClass() {
		return openClass;
	}
	public IRuntimeEnv getEnv() {
		return env;
	}
	
	
}
