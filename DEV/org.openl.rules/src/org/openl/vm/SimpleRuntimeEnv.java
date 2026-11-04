package org.openl.vm;

import java.lang.ref.SoftReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.RecursiveAction;

import lombok.Getter;
import lombok.Setter;

import org.openl.IOpenRunner;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.lang.xls.binding.wrapper.IRulesMethodWrapper;
import org.openl.runtime.IRuntimeContext;
import org.openl.types.IOpenClass;

public class SimpleRuntimeEnv implements IRuntimeEnv {

    private static final Object[] NO_PARAMS = {};
    // Shared by the environments that have checked no alias value yet
    private static final SoftReference<CheckedAliasValues> NO_ALIAS_VALUES = new SoftReference<>(null);
    private static final Object NULL_THIS = new Object();

    @Getter
    private final IOpenRunner runner;
    protected final ArrayDeque<Object> thisStack = new ArrayDeque<>();
    protected final ArrayDeque<Object[]> frameStack = new ArrayDeque<>();
    protected Deque<IRuntimeContext> contextStack;
    @Getter
    @Setter
    private IOpenClass topClass;
    @Getter
    @Setter
    private IRulesMethodWrapper methodWrapper;
    private Queue<RecursiveAction> actionStack;
    @Getter
    private Tracer tracer = Tracer.NONE;
    // Replaced on the first alias value an execution checks
    private SoftReference<CheckedAliasValues> checkedAliasValues = NO_ALIAS_VALUES;

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

    public Deque<IRuntimeContext> cloneContextStack() {
        return new ArrayDeque<>(contextStack);
    }

    private IRuntimeContext buildDefaultRuntimeContext() {
        return RulesRuntimeContextFactory.buildRulesRuntimeContext();
    }


    public void pushAction(RecursiveAction action) {
        if (actionStack == null) {
            actionStack = new LinkedList<>();
        }
        actionStack.add(action);
    }

    public boolean joinActionIfExists() {
        if (actionStack != null && !actionStack.isEmpty()) {
            var action = actionStack.poll();
            action.join();
            return true;
        }
        return false;
    }

    public boolean cancelActionIfExists() {
        if (actionStack != null && !actionStack.isEmpty()) {
            var action = actionStack.poll();
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
        this.tracer = env.tracer;
        // A copy starts a run of its own: the method in flight and the actions it forked stay with the original.
        this.topClass = null;
        this.methodWrapper = null;
        this.actionStack = null;
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

    // Public method kept for compatibility; it delegates to copy(), the copy constructor.
    // copy() builds the copy through the copy constructor, so super.clone() is not called.
    @SuppressWarnings({"java:S2975", "java:S1182"})
    @Override
    public IRuntimeEnv clone() {
        return copy();
    }

    @Override
    public SimpleRuntimeEnv copy() {
        return new SimpleRuntimeEnv(this);
    }

    @Override
    public void setTracer(Tracer tracer) {
        this.tracer = tracer == null ? Tracer.NONE : tracer;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The values are let go when memory runs short, and a copy of the environment starts without them.
     */
    @Override
    public CheckedAliasValues getCheckedAliasValues() {
        var values = checkedAliasValues.get();
        if (values == null) {
            values = new CheckedAliasValues();
            checkedAliasValues = new SoftReference<>(values);
        }
        return values;
    }
}
