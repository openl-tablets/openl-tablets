package org.openl.types.impl;

import org.openl.IOpenRunner;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.BlockNode;
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
    
    private IBoundNode expressionNode; 

    public CompositeMethodInvoker(IBoundMethodNode methodBodyBoundNode, CompositeMethod method) {        
        this.methodBodyBoundNode = methodBodyBoundNode;
        
        optimizeMethodCall(methodBodyBoundNode, method);
    }

    private void optimizeMethodCall(IBoundMethodNode methodBodyBoundNode,
			CompositeMethod method) {
    	
    	if (methodBodyBoundNode instanceof BlockNode) {
			BlockNode mbb = (BlockNode) methodBodyBoundNode;
			if (mbb.getChildren().length == 1 && mbb.getLocalFrameSize() == method.getSignature().getNumberOfParameters())
			{
				expressionNode = mbb.getChildren()[0];
			}	
		}
    	
    	
    	
	}

	public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        try {
            env.pushThis(target);
            IOpenRunner runner = env.getRunner();

            return expressionNode == null ? runner.run(methodBodyBoundNode, params, env) : runner.runExpression(expressionNode, params, env);
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
