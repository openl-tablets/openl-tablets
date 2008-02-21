/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.binding.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class BinaryOpNodeOr extends BinaryOpNode
{
  /**
   * @param syntaxNode
   * @param child
   * @param method
   */
  public BinaryOpNodeOr(
    ISyntaxNode syntaxNode,
    IBoundNode[] child,
    IMethodCaller method)
  {
    super(syntaxNode, child, method);
  }

	public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException
	{
		
		if (children[0].getType().getInstanceClass() == boolean.class)
		{
			Boolean b1 = (Boolean)children[0].evaluate(env);
			if (!b1.booleanValue())
				return children[1].evaluate(env);
			return Boolean.TRUE; 
		}	
		
		return super.evaluateRuntime(env);
	}



}
