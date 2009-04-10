/*
 * Created on May 29, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.binding.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class TypeBoundNode extends ABoundNode
{
	protected IOpenClass type;


  /**
   * @param syntaxNode
   * @param children
   */
  public TypeBoundNode(ISyntaxNode syntaxNode, IOpenClass type)
  {
    super(syntaxNode,IBoundNode.EMPTY);
    this.type = type;
  }

  /* (non-Javadoc)
   * @see org.openl.binding.IBoundNode#evaluate(java.lang.Object, java.lang.Object[], org.openl.env.IRuntimeEnv)
   */
//  public Object evaluate(Object target, Object[] pars, IRuntimeEnv env)
//  {
//    throw new UnsupportedOperationException("TypeNode can not be evaluated");
//  }
//


  /* (non-Javadoc)
   * @see org.openl.binding.IBoundNode#getType()
   */
  public IOpenClass getType()
  {
    return type;
  }

  /* (non-Javadoc)
   * @see org.openl.binding.IBoundNode#evaluate(org.openl.vm.IRuntimeEnv)
   */
	public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException
  {
  	//TODO probably create another class for static method access  
//		throw new UnsupportedOperationException("TypeNode can not be evaluated");
		return null;
  }

	public void updateDependency(BindingDependencies dependencies)
	{
		dependencies.addTypeDependency(type, this);
	}
	
	

}
