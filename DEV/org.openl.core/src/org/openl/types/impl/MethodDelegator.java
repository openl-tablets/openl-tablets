package org.openl.types.impl;

import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class MethodDelegator implements IOpenMethod {

	protected IMethodCaller methodCaller;
	
	public MethodDelegator(IMethodCaller methodCaller) {
		this.methodCaller = methodCaller;
	}
	
	public IMethodSignature getSignature() {
		return methodCaller.getMethod().getSignature();
	}

	public IOpenClass getDeclaringClass() {
		return methodCaller.getMethod().getDeclaringClass();
	}

	public IMemberMetaInfo getInfo() {
		return methodCaller.getMethod().getInfo();
	}

	public IOpenClass getType() {
		return methodCaller.getMethod().getType();
	}

	public boolean isStatic() {
		return methodCaller.getMethod().isStatic();
	}

	public String getDisplayName(int mode) {
		return methodCaller.getMethod().getDisplayName(mode);
	}

	public String getName() {
		return methodCaller.getMethod().getName();
	}

	public IOpenMethod getMethod() {
		return methodCaller.getMethod();
	}

	public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
		return methodCaller.invoke(target, params, env);
	}
	
	@Override
	public boolean isConstructor() {
	    return methodCaller.getMethod().isConstructor();
	}
}
