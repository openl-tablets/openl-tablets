package com.exigen.openl.component;

import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class OpenInstance {
	IOpenClass openClass;
	Object instance;
	IRuntimeEnv env;
	public OpenInstance(IOpenClass openClass, Object instance, IRuntimeEnv env) {
		super();
		this.openClass = openClass;
		this.instance = instance;
		this.env = env;
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
