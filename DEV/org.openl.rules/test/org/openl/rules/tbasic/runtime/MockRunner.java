package org.openl.rules.tbasic.runtime;

import org.openl.IOpenRunner;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.vm.IRuntimeEnv;

public class MockRunner implements IOpenRunner {

    @Override
    public Object run(IBoundMethodNode node, Object[] params) throws OpenLRuntimeException {
        int frameSize = node.getLocalFrameSize();

        return node.evaluate(new MockRuntimeEnv(this, frameSize, params));
    }

    @Override
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
}
