package org.openl.vm;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.RecursiveAction;

import org.openl.IOpenRunner;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.lang.xls.binding.wrapper.IRulesMethodWrapper;
import org.openl.runtime.IRuntimeContext;
import org.openl.types.IOpenClass;

public class SimpleRuntimeEnv implements IRuntimeEnv {

    private static final Object[] NO_PARAMS = {};
    private static final Object NULL_THIS = new Object();

    private final IOpenRunner runner;
    protected final ArrayDeque<Object> thisStack = new ArrayDeque<>();
    protected final ArrayDeque<Object[]> frameStack = new ArrayDeque<>();
    protected ArrayDeque<IRuntimeContext> contextStack;
    private IOpenClass topClass;
    private IRulesMethodWrapper methodWrapper;
    private Queue<RecursiveAction> actionStack = null;

    public SimpleRuntimeEnv() {
        this.runner = SimpleRunner.SIMPLE_RUNNER;
        this.contextStack = new ArrayDeque<>();
        pushLocalFrame(NO_PARAMS);
        pushContext(buildDefaultRuntimeContext());
    }

    SimpleRuntimeEnv(IOpenRunner runner, int frameSize, Object[] params) {
        Object[] aLocalFrame = new Object[frameSize];
        this.runner = runner;
        contextStack = new ArrayDeque<>();
        System.arraycopy(params, 0, aLocalFrame, 0, params.length);
        pushLocalFrame(aLocalFrame);
        pushContext(buildDefaultRuntimeContext());
    }

    public ArrayDeque<IRuntimeContext> cloneContextStack() {
        return new ArrayDeque<>(contextStack);
    }

    private IRuntimeContext buildDefaultRuntimeContext() {
        return RulesRuntimeContextFactory.buildRulesRuntimeContext();
    }

    public IRulesMethodWrapper getMethodWrapper() {
        return methodWrapper;
    }

    public void setMethodWrapper(IRulesMethodWrapper methodWrapper) {
        this.methodWrapper = methodWrapper;
    }

    public IOpenClass getTopClass() {
        return topClass;
    }

    public void setTopClass(IOpenClass topClass) {
        this.topClass = topClass;
    }


    public void pushAction(RecursiveAction action) {
        if (actionStack == null) {
            actionStack = new LinkedList<>();
        }
        actionStack.add(action);
    }

    public boolean joinActionIfExists() {
        if (actionStack != null && !actionStack.isEmpty()) {
            RecursiveAction action = actionStack.poll();
            action.join();
            return true;
        }
        return false;
    }

    public boolean cancelActionIfExists() {
        if (actionStack != null && !actionStack.isEmpty()) {
            RecursiveAction action = actionStack.poll();
            action.cancel(true);
            return true;
        }
        return false;
    }

    public SimpleRuntimeEnv(SimpleRuntimeEnv env) {
        this.runner = SimpleRunner.SIMPLE_RUNNER;
        contextStack = new ArrayDeque<>(env.contextStack);
        pushThis(env.getThis());
        pushLocalFrame(env.getLocalFrame());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.vm.IRuntimeEnv#getLocalFrame()
     */
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
        return thisStack.peek();
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
        thisStack.push(thisObject == null ? NULL_THIS : thisObject); // To prevent NPE
    }

    @Override
    public IRuntimeContext getContext() {
        return contextStack.peek();
    }

    @Override
    public void setContext(IRuntimeContext context) {
        if (context == null) {
            context = buildDefaultRuntimeContext();
        }
        contextStack.clear();
        pushContext(context);
    }

    @Override
    public IRuntimeContext popContext() {
        return contextStack.pop();
    }

    @Override
    public void pushContext(IRuntimeContext context) {
        contextStack.push(context);
    }

    @Override
    public boolean isContextManagingSupported() {
        return true;
    }

    @Override
    public IRuntimeEnv clone() {
        return copy();
    }

    @Override
    public SimpleRuntimeEnv copy() {
        return new SimpleRuntimeEnv(this);
    }
}
