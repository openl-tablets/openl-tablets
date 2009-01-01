/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.binding.impl;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundNode;
import org.openl.binding.IBoundNodeVisitor;
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

	public boolean visit(IBoundNodeVisitor visitor) 
	{
		if (!visitor.visit(this))
			return false;
		if (children == null)
			return true;
		for (int i = 0; i < children.length; i++) 
		{
			if (!children[i].visit(visitor))
				return false;
		}
		return true;
	}



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

  public IBoundNode[] getChildren()
  {
    return children;
  }
  
  public IBoundNode getTargetNode()
  {
  	return null;
  }


  public void assign(Object value, IRuntimeEnv env) throws OpenLRuntimeException
  {
		throw new UnsupportedOperationException();
  }

  public boolean isLvalue()
  {
    return false;
  }

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
