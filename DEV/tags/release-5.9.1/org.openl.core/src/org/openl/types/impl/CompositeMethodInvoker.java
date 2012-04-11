package org.openl.types.impl;

import org.openl.IOpenRunner;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.impl.ControlSignalReturn;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

/**
 * Invoker for {@link CompositeMethod}.
 * 
 * @author DLiauchuk
 *
 */
public class CompositeMethodInvoker implements Invokable {

    private IBoundMethodNode methodBodyBoundNode;

    public CompositeMethodInvoker(IBoundMethodNode methodBodyBoundNode) {        
        this.methodBodyBoundNode = methodBodyBoundNode;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        try {
            env.pushThis(target);
            IOpenRunner runner = env.getRunner();

            return runner.run(methodBodyBoundNode, params, env);
        } catch (ControlSignalReturn csret) {
            return csret.getReturnValue();
        } finally {
            env.popThis();
        }
    }

    protected Object getMethodBodyBoundNode() {        
        return methodBodyBoundNode;
    }
}
