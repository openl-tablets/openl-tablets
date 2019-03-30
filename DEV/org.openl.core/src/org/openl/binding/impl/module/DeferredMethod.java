/*
 * Created on Jul 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import org.openl.IOpenRunner;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.impl.ControlSignalReturn;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.impl.AMethod;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class DeferredMethod extends AMethod {
    private ISyntaxNode methodBodyNode;

    private IBoundMethodNode methodBodyBoundNode = null;
    
    /**
     * @param name
     * @param typeClass
     * @param parameterTypes
     * @param declaringClass
     */
    public DeferredMethod(String name, IOpenClass typeClass, IMethodSignature signature, IOpenClass declaringClass,
            ISyntaxNode methodBodyNode) {
        super(new OpenMethodHeader(name, typeClass, signature, declaringClass));
        this.methodBodyNode = methodBodyNode;
    }

    /**
     * @return
     */
    public ISyntaxNode getMethodBodyNode() {
        return methodBodyNode;
    }

    @Override
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

    public void setMethodBodyBoundNode(IBoundMethodNode bnode) {
        methodBodyBoundNode = bnode;
    }
    
    @Override
    public boolean isConstructor() {
        return false;
    }
}
