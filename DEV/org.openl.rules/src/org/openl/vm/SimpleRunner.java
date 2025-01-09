package org.openl.vm;

import org.openl.IOpenRunner;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;

class SimpleRunner implements IOpenRunner {

    static final SimpleRunner SIMPLE_RUNNER = new SimpleRunner();

    private SimpleRunner() {
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.IOpenRunner#run(java.lang.Object[])
     */
    @Override
    public Object run(IBoundMethodNode node, Object[] params) {
        int frameSize = node.getLocalFrameSize();

        return node.evaluate(new SimpleVM.SimpleRuntimeEnv(this, frameSize, params));
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
