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
import org.openl.exception.OpenlNotCheckedException;
import org.openl.runtime.DefaultRuntimeContext;
import org.openl.runtime.IRuntimeContext;
import org.openl.util.fast.FastStack;

/**
 * @author snshor
 * 
 */
public class SimpleVM implements IOpenVM {

    static class SimpleRunner implements IOpenRunner {

        SimpleRunner() {
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.openl.IOpenRunner#run(java.lang.Object[])
         */
        public Object run(IBoundMethodNode node, Object[] params) throws OpenLRuntimeException {
            int frameSize = node.getLocalFrameSize();

            return node.evaluate(new SimpleRuntimeEnv(this, frameSize, params));
        }

        public Object run(IBoundMethodNode node, Object[] params, IRuntimeEnv env) throws OpenLRuntimeException {
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

        // public Object run2(Object[] params)
        // {
        //
        // return evaluate(node, new SimpleRuntimeEnv(frameSize));
        // }

        // Object evaluate(IBoundNode bnode, IRuntimeEnv env)
        // {
        //
        // IBoundNode targetNode = bnode.getTargetNode();
        // Object target = targetNode == null ? null : evaluate(targetNode,
        // env);
        //
        //
        // IBoundNode[] children = bnode.getChildren();
        // Object[] res = null;
        // if (children != null)
        // {
        // res = new Object[children.length];
        // for (int i = 0; i < res.length; i++)
        // {
        // res[i] = evaluate(children[i], env);
        // }
        // }
        // return bnode.evaluate(target != null ? target : env, res, env);
        // }

    }

    public static class SimpleRuntimeEnv implements IRuntimeEnv {

        IOpenRunner runner;

        FastStack thisStack = new FastStack(100);

        FastStack frameStack = new FastStack(100);

        private FastStack contextStack = new FastStack(5);

        public SimpleRuntimeEnv() {
            this(new SimpleRunner(), 0, new Object[] {});
        }

        SimpleRuntimeEnv(IOpenRunner runner, int frameSize, Object[] params) {
            Object[] aLocalFrame = new Object[frameSize];
            this.runner = runner;

            System.arraycopy(params, 0, aLocalFrame, 0, params.length);
            pushLocalFrame(aLocalFrame);

            pushContext(buildDefaultRuntimeContext());
        }

        protected IRuntimeContext buildDefaultRuntimeContext() {
            return new DefaultRuntimeContext();
        }

        public SimpleRuntimeEnv(SimpleRuntimeEnv env) {
            this();
            pushThis(env.getThis());
            pushContext(env.getContext());
            pushLocalFrame(env.getLocalFrame());
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.openl.vm.IRuntimeEnv#getLocalFrame()
         */
        public Object[] getLocalFrame() {
            return (Object[]) frameStack.peek();
        }

        public IOpenRunner getRunner() {
            return runner;
        }

        public Object getThis() {
            if (thisStack.size() == 0) {
                return null;
            }
            return thisStack.peek();
        }

        public Object[] popLocalFrame() {
            return (Object[]) frameStack.pop();
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
            if (contextStack.size() > 0) {
                return (IRuntimeContext) contextStack.peek();
            }

            throw new IllegalStateException("Context stack is empty!");
        }

        public void setContext(IRuntimeContext context) {
            if (context == null) {
                context = buildDefaultRuntimeContext();
            }
            contextStack.clear();
            pushContext(context);
        }

        public IRuntimeContext popContext() {
            if (contextStack.size() > 0) {
                return (IRuntimeContext) contextStack.pop();
            } else {
                throw new OpenlNotCheckedException(
                    "Failed to restore context. The context modification history is empty.");
            }
        }

        public void pushContext(IRuntimeContext context) {
            contextStack.push(context);
        }

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
    public IOpenRunner getRunner() {
        return new SimpleRunner();
    }

    public IRuntimeEnv getRuntimeEnv() {
        return new SimpleRuntimeEnv();
    }

}
