package org.openl.rules.dt.algorithm2;

import org.openl.vm.IRuntimeEnv;

public class RuntimeContext {
	
	public RuntimeContext(Object target, Object[] params, IRuntimeEnv env) {
		super();
		this.target = target;
		this.params = params;
		this.env = env;
	}
	public Object target; 
	public Object[] params; 
	public IRuntimeEnv env;

}
