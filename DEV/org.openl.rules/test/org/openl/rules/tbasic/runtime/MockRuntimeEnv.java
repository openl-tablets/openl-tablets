package org.openl.rules.tbasic.runtime;

import java.util.Stack;

import org.openl.IOpenRunner;
import org.openl.runtime.IRuntimeContext;
import org.openl.vm.IRuntimeEnv;

public class MockRuntimeEnv implements IRuntimeEnv {
	private final IOpenRunner runner;
	
	private final Stack<Object> thisStack = new Stack<Object>();
	private final Stack<Object[]> frameStack = new Stack<Object[]>();
	
	private IRuntimeContext context;
	
	public MockRuntimeEnv(MockRuntimeEnv mockRuntimeEnv) {
		this(new MockRunner(), 0, new Object[] {});
		this.pushThis(mockRuntimeEnv.getThis());
		this.pushLocalFrame(this.getLocalFrame());
	}
	
	public MockRuntimeEnv(IOpenRunner runner, int frameSize, Object[] params) {
		this.runner = runner;
		
		Object[] aLocalFrame = new Object[frameSize];
		System.arraycopy(params, 0, aLocalFrame, 0, params.length);
		
		pushLocalFrame(aLocalFrame);
	}
	
	@Override
    public Object[] getLocalFrame() {
		return frameStack.peek();
	}
	
	@Override
    public IOpenRunner getRunner() {
		return runner;
	}
	
	@Override
    public Object getThis() {
		if (thisStack.isEmpty()) {
			return null;
		} else {
			return thisStack.peek();
		}
	}
	
	@Override
    public Object[] popLocalFrame() {
		return frameStack.pop();
	}
	
	@Override
    public Object popThis() {
		return thisStack.pop();
	}
	
	@Override
    public void pushLocalFrame(Object[] frame) {
		frameStack.push(frame);
	}
	
	@Override
    public void pushThis(Object thisObject) {
		thisStack.push(thisObject);
	}
	
	@Override
    public IRuntimeContext getContext() {
		return context;
	}
	
	@Override
    public void setContext(IRuntimeContext context) {
		this.context = context;
	}

    @Override
    public boolean isContextManagingSupported() {
        return false;
    }

    @Override
    public IRuntimeContext popContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void pushContext(IRuntimeContext context) {
        throw new UnsupportedOperationException();
    }

	@Override
	public IRuntimeEnv clone() {
		return new MockRuntimeEnv(this);
	
	}
}
