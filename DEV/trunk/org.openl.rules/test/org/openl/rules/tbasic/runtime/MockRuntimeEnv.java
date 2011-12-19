package org.openl.rules.tbasic.runtime;

import java.util.Stack;

import org.apache.commons.lang.NotImplementedException;
import org.openl.IOpenRunner;
import org.openl.runtime.IRuntimeContext;
import org.openl.vm.IRuntimeEnv;

public class MockRuntimeEnv implements IRuntimeEnv {
	private final IOpenRunner runner;
	
	private final Stack<Object> thisStack = new Stack<Object>();
	private final Stack<Object[]> frameStack = new Stack<Object[]>();
	
	private IRuntimeContext context;
	
	public MockRuntimeEnv() {
		this(new MockRunner(), 0, new Object[] {});
	}
	
	public MockRuntimeEnv(IOpenRunner runner, int frameSize, Object[] params) {
		this.runner = runner;
		
		Object[] aLocalFrame = new Object[frameSize];
		System.arraycopy(params, 0, aLocalFrame, 0, params.length);
		
		pushLocalFrame(aLocalFrame);
	}
	
	public Object[] getLocalFrame() {
		return frameStack.peek();
	}
	
	public IOpenRunner getRunner() {
		return runner;
	}
	
	public Object getThis() {
		if (thisStack.isEmpty()) {
			return null;
		} else {
			return thisStack.peek();
		}
	}
	
	public Object[] popLocalFrame() {
		return frameStack.pop();
	}
	
	public Object popThis() {
		return thisStack.pop();
	}
	
	public void pushLocalFrame(Object[] frame) {
		frameStack.push(frame);
	}
	
	public void pushThis(Object thisObject) {
		thisStack.push(thisObject);
	}
	
	public IRuntimeContext getContext() {
		return context;
	}
	
	public void setContext(IRuntimeContext context) {
		this.context = context;
	}

    @Override
    public boolean isContextManagingSupported() {
        return false;
    }

    @Override
    public IRuntimeContext popContext() {
        throw new NotImplementedException();
    }

    @Override
    public void pushContext(IRuntimeContext context) {
        throw new NotImplementedException();
    }
}
