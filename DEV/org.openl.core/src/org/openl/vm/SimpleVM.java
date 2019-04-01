/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.vm;

import org.openl.IOpenRunner;
import org.openl.IOpenVM;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.runtime.DefaultRuntimeContext;
import org.openl.runtime.IRuntimeContext;
import org.openl.util.fast.FastStack;

/**
 * @author snshor
 *
 */
public class SimpleVM implements IOpenVM {

    private static final SimpleRunner SIMPLE_RUNNER = new SimpleRunner();

    static class SimpleRunner implements IOpenRunner {

        SimpleRunner() {
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.IOpenRunner#run(java.lang.Object[])
         */
        @Override
        public Object run(IBoundMethodNode node, Object[] params) {
            int frameSize = node.getLocalFrameSize();

            return node.evaluate(new SimpleRuntimeEnv(this, frameSize, params));
        }

        @Override
        public Object run(IBoundMethodNode node, Object[] params, IRuntimeEnv env) {
            int frameSize = node.getLocalFrameSize();

            Object[] frame = new Object[frameSize];

            if (params != null && params.length > 0) {
                System.arraycopy(params, 0, frame, 0, params.length);
            }

            try {
                env.pushLocalFrame(frame);
                return node.evaluate(env);
            } finally {
                env.popLocalFrame();
            }
        }

        @Override
        public Object runExpression(IBoundNode expressionNode, Object[] params, IRuntimeEnv env) {
            try {
                env.pushLocalFrame(params);
                return expressionNode.evaluate(env);
            } finally {
                env.popLocalFrame();
            }
        }
    }

    public static class SimpleRuntimeEnv implements IRuntimeEnv {

        IOpenRunner runner;

        protected FastStack thisStack = new FastStack(100);

        protected FastStack frameStack = new FastStack(100);

        protected FastStack contextStack;

        public SimpleRuntimeEnv() {
            this(SIMPLE_RUNNER, 0, new Object[] {});
        }

        SimpleRuntimeEnv(IOpenRunner runner, int frameSize, Object[] params) {
            Object[] aLocalFrame = new Object[frameSize];
            this.runner = runner;
            contextStack = new FastStack(5);
            System.arraycopy(params, 0, aLocalFrame, 0, params.length);
            pushLocalFrame(aLocalFrame);
            pushContext(buildDefaultRuntimeContext());
        }

        protected IRuntimeContext buildDefaultRuntimeContext() {
            return new DefaultRuntimeContext();
        }

        public SimpleRuntimeEnv(SimpleRuntimeEnv env) {
            this.runner = SIMPLE_RUNNER;
            contextStack = (FastStack) env.contextStack.clone();
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
            return (Object[]) frameStack.peek();
        }

        @Override
        public IOpenRunner getRunner() {
            return runner;
        }

        @Override
        public Object getThis() {
            if (thisStack.size() == 0) {
                return null;
            }
            return thisStack.peek();
        }

        @Override
        public Object[] popLocalFrame() {
            return (Object[]) frameStack.pop();
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
            if (contextStack.size() > 0) {
                return (IRuntimeContext) contextStack.peek();
            }

            throw new IllegalStateException("Context stack is empty!");
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
            if (contextStack.size() > 0) {
                return (IRuntimeContext) contextStack.pop();
            } else {
                throw new OpenLRuntimeException(
                    "Failed to restore context. The context modification history is empty.");
            }
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
            return new SimpleRuntimeEnv(this);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenVM#run(org.openl.binding.IBoundCode)
     */
    @Override
    public IOpenRunner getRunner() {
        return SIMPLE_RUNNER;
    }

    @Override
    public IRuntimeEnv getRuntimeEnv() {
        return new SimpleRuntimeEnv();
    }

}
