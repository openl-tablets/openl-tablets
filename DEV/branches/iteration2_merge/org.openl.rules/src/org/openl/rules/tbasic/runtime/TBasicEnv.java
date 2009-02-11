/**
 * 
 */
package org.openl.rules.tbasic.runtime;

import org.openl.IOpenRunner;
import org.openl.types.impl.DelegatedDynamicObject;
import org.openl.vm.IRuntimeEnv;

public class TBasicEnv implements IRuntimeEnv
{
	IRuntimeEnv env;
	TBasicVM tbasicVm;
	DelegatedDynamicObject tbasicTarget;
	

	public IRuntimeEnv getEnv() {
		return env;
	}

	public TBasicVM getTbasicVm() {
		return tbasicVm;
	}

	public DelegatedDynamicObject getTbasicTarget() {
		return tbasicTarget;
	}

	public Object[] getLocalFrame() {
		return env.getLocalFrame();
	}

	public IOpenRunner getRunner() {
		return env.getRunner();
	}

	public Object getThis() {
		return env.getThis();
	}

	public Object[] popLocalFrame() {
		return env.popLocalFrame();
	}

	public Object popThis() {
		return env.popThis();
	}

	public void pushLocalFrame(Object[] frame) {
		env.pushLocalFrame(frame);
	}

	public void pushThis(Object thisObject) {
		env.pushThis(thisObject);
	}

	public TBasicEnv(IRuntimeEnv env, TBasicVM tbasicVm, DelegatedDynamicObject tbasicTarget) {
		super();
		this.env = env;
		this.tbasicVm = tbasicVm;
		this.tbasicTarget = tbasicTarget;
	}
}