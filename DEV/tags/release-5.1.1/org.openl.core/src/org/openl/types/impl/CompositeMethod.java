/*
 * Created on Oct 8, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.types.impl;

import org.openl.IOpenRunner;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.impl.ControlSignalReturn;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;


public class CompositeMethod extends AMethod
{
	IBoundMethodNode methodBodyBoundNode;
	
	public CompositeMethod(IOpenMethodHeader header,
	IBoundMethodNode methodBodyBoundNode)
	{
		super(header);
		this.methodBodyBoundNode = methodBodyBoundNode;
	}
	

	public Object invoke(Object target, Object[] params, IRuntimeEnv env)
	{
		try
		{
			env.pushThis(target);
			IOpenRunner runner = env.getRunner();
	
			return runner.run(methodBodyBoundNode, params, env); 
		}
		catch(ControlSignalReturn csret)
		{
			return csret.getReturnValue();
		}
		finally
		{
			env.popThis();
		}
	
	
	}

  /**
   * @return
   */
  public IBoundMethodNode getMethodBodyBoundNode()
  {
    return methodBodyBoundNode;
  }

  /**
   * @param node
   */
  public void setMethodBodyBoundNode(IBoundMethodNode node)
  {
    methodBodyBoundNode = node;
  }


	/**
	 * @param dependencies
	 */
	public void updateDependency(BindingDependencies dependencies)
	{
		dependencies.visit(getMethodBodyBoundNode());
	}
	
	public IOpenClass getBodyType()
	{
		return methodBodyBoundNode.getType();
	}

}