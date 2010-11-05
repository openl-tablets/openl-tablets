/*
 * Created on Oct 8, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundMethodNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

public class CompositeMethod extends ExecutableRulesMethod {
    
    private IBoundMethodNode methodBodyBoundNode;
    
    /**
     * Invoker for current method.
     */
    private Invokable invoker;

    public CompositeMethod(IOpenMethodHeader header, IBoundMethodNode methodBodyBoundNode) {
        super(header);
        this.methodBodyBoundNode = methodBodyBoundNode;
    }

    public IOpenClass getBodyType() {
        return methodBodyBoundNode.getType();
    }
   
    public IBoundMethodNode getMethodBodyBoundNode() {
        return methodBodyBoundNode;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        if (invoker == null) {
            // create new instance of invoker.
            invoker = new CompositeMethodInvoker(methodBodyBoundNode);
        } 
        return invoker.invoke(target, params, env);
    }

    public void setMethodBodyBoundNode(IBoundMethodNode node) {
        methodBodyBoundNode = node;
    }
        
    public void updateDependency(BindingDependencies dependencies) {
        dependencies.visit(getMethodBodyBoundNode());
    }

	@Override
	public BindingDependencies getDependencies() {
		BindingDependencies dependencies = new BindingDependencies();
		updateDependency(dependencies);
		return dependencies;
	}

	@Override
	public Map<String, Object> getProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ISyntaxNode getSyntaxNode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSourceUrl() {
		// TODO Auto-generated method stub
		return null;
	}
  
}