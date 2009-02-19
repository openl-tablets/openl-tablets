/**
 * 
 */
package org.openl.rules.tbasic.runtime;

import org.openl.IOpenRunner;
import org.openl.types.impl.DelegatedDynamicObject;
import org.openl.vm.IRuntimeEnv;

public class TBasicContextHolderEnv implements IRuntimeEnv
{
	private IRuntimeEnv env;
	private TBasicVM tbasicVm;
	private DelegatedDynamicObject tbasicTarget;
    private Object[] tbasicParams;
	

	public IRuntimeEnv getEnv() {
		return env;
	}

	public TBasicVM getTbasicVm() {
		return tbasicVm;
	}

	public DelegatedDynamicObject getTbasicTarget() {
		return tbasicTarget;
	}
	
    /**
     * @return the tbasicParams
     */
    public Object[] getTbasicParams() {
        return tbasicParams;
    }
    
    public TBasicContextHolderEnv(IRuntimeEnv env, DelegatedDynamicObject tbasicTarget, Object[] params,
            TBasicVM tbasicVM) {
        super();
        this.env = env;
        this.tbasicVm = tbasicVM;
        this.tbasicParams = params;
        this.tbasicTarget = tbasicTarget;
    }
    
    public void assignValueToVariable(String variableName, Object value) {
        tbasicTarget.setFieldValue(variableName, value);
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
}