package org.openl.types.impl;

import org.openl.IOpenRunner;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.impl.ControlSignalReturn;
import org.openl.vm.IRuntimeEnv;

/**
 * Invoker for {@link CompositeMethod}.
 * 
 * @author DLiauchuk
 *
 */
public class CompositeMethodInvoker extends Invoker {

    private IBoundMethodNode methodBodyBoundNode;

    public CompositeMethodInvoker(IBoundMethodNode methodBodyBoundNode, Object target, Object[] params, IRuntimeEnv env) {
        super(target, params, env);
        this.methodBodyBoundNode = methodBodyBoundNode;
    }

    public Object invoke() {
        try {
            getEnv().pushThis(getTarget());
            IOpenRunner runner = getEnv().getRunner();

            return runner.run(methodBodyBoundNode, getParams(), getEnv());
        } catch (ControlSignalReturn csret) {
            return csret.getReturnValue();
        } finally {
            getEnv().popThis();
        }
    }

    protected Object getMethodBodyBoundNode() {        
        return methodBodyBoundNode;
    }
}
