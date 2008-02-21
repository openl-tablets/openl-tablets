/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.binding.OpenLRuntimeException;
import org.openl.binding.impl.ATargetBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class MethodBoundNode extends ATargetBoundNode
{

	protected IMethodCaller boundMethod;
	
	public MethodBoundNode(ISyntaxNode syntaxNode, IBoundNode[] child, IMethodCaller method)
	{
		super(syntaxNode, child);
		this.boundMethod = method;
	}

	public MethodBoundNode(ISyntaxNode syntaxNode, IBoundNode[] child, IMethodCaller method, IBoundNode targetNode)
	{
		super(syntaxNode, child, targetNode);
		this.boundMethod = method;
	}


  /* (non-Javadoc)
   * @see org.openl.binding.IBoundNode#getType()
   */
  public IOpenClass getType()
  {
    return boundMethod.getMethod().getType();
  }

//  /* (non-Javadoc)
//   * @see org.openl.binding.IBoundNode#invoke(java.lang.Object[])
//   */
//  public Object evaluate(Object target, Object[] pars, IRuntimeEnv env)
//  {
//    return boundMethod.invoke(target, pars);
//  }
  
  
	/* (non-Javadoc)
	 * @see org.openl.binding.IBoundNode#evaluate(org.openl.vm.IRuntimeEnv)
	 */
	public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException
	{

		try
		{
			Object target = targetNode == null ? env.getThis() : targetNode.evaluate(env);
			Object[] pars = evaluateChildren(env);
			return boundMethod.invoke(target, pars, env);		
		}
		catch (ControlSignalReturn signal)
		{
			return signal.getReturnValue();
		}
		catch(OpenLRuntimeException opex)
		{
			opex.pushMethodNode(this);
			throw opex;
		}


	}

	public void updateDependency(BindingDependencies dependencies)
	{
		dependencies.addMethodDependency(boundMethod.getMethod(), this);
	}


  

}
