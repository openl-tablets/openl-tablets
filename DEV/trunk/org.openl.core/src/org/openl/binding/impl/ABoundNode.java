/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.binding.OpenLRuntimeException;
import org.openl.binding.impl.ControlSignal;
import org.openl.syntax.ISyntaxNode;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public abstract class ABoundNode implements IBoundNode
{

	protected ISyntaxNode syntaxNode;
	protected IBoundNode[] children;

	protected ABoundNode(ISyntaxNode syntaxNode, IBoundNode[] children)
	{
		this.syntaxNode = syntaxNode;
		this.children = children;
	}
	


	public Object[] evaluateChildren(IRuntimeEnv env) throws OpenLRuntimeException
	{
		if (children == null)
		{
			return null;
		}
		
		Object[] ch = new Object[children.length];
		
		for (int i = 0; i < ch.length; i++)
    {
      ch[i] = children[i].evaluate(env);
    }
		
		return ch;
	}

		
	

	
  /* (non-Javadoc)
   * @see org.openl.binding.IBoundNode#getSyntaxNode()
   */
  public ISyntaxNode getSyntaxNode()
  {
    return syntaxNode;
  }

  /* (non-Javadoc)
   * @see org.openl.binding.IBoundNode#getChild()
   */
  public IBoundNode[] getChildren()
  {
    return children;
  }
  
  public IBoundNode getTargetNode()
  {
  	return null;
  }


  /* (non-Javadoc)
   * @see org.openl.binding.IBoundNode#assign(java.lang.Object)
   */
  public void assign(Object value, IRuntimeEnv env) throws OpenLRuntimeException
  {
		throw new UnsupportedOperationException();
  }

  /* (non-Javadoc)
   * @see org.openl.binding.IBoundNode#isLvalue()
   */
  public boolean isLvalue()
  {
    return false;
  }

  /* (non-Javadoc)
   * @see org.openl.binding.IBoundNode#evaluate(org.openl.vm.IRuntimeEnv)
   */
  public Object evaluate(IRuntimeEnv env) throws OpenLRuntimeException
  {
  	try
  	{
  		return evaluateRuntime(env);
  	}
  	catch(OpenLRuntimeException ore)
  	{
  		throw ore;
  	}
  	catch(ControlSignal controlSignal)
  	{
  		throw controlSignal;
  	}
  	catch(Throwable t)
  	{
  		throw new OpenLRuntimeException(t, this);
  	}
  	
  }



	public void updateAssignFieldDependency(BindingDependencies dependencies)
	{
		// do nothing
		
	}



	public void updateDependency(BindingDependencies dependencies)
	{
		// do nothing
		
	}

  
  
  
}
