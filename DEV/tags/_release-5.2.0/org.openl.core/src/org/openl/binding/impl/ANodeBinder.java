/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.INodeBinder;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.SyntaxError;
import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;



/**
 * @author snshor
 *
 */
public abstract class ANodeBinder implements INodeBinder
{

	static protected IBoundNode[] bindChildren(ISyntaxNode parentNode, IBindingContext bindingContext)
	  throws BoundError
	{
		return bindChildren(parentNode, bindingContext, 0, parentNode.getNumberOfChildren());	
	}
		
	static protected IBoundNode[] bindChildren(ISyntaxNode parentNode, IBindingContext bindingContext, int from , int to)
	  throws  BoundError
	{
		int n = to - from;
		
		if (n == 0)
		  return IBoundNode.EMPTY;
		
		IBoundNode[] children = new IBoundNode[n];  
		
		
		int noProblems = 0;
		
		for (int i = 0; i < n; i++)
    {
    	ISyntaxNode childNode = parentNode.getChild(from + i);
    	if (childNode == null)
    	{
    		++noProblems;
    		continue;
    	} 
      INodeBinder binder = bindingContext.findBinder(childNode);
      if (binder == null)
      {
      	bindingContext.addError(new BoundError(childNode, "Can not find binder for node:" + childNode.getType(), null));
      	children[i] = new ErrorBoundNode(childNode);
      }
      else
      {
      	try
        {
					children[i] = binder.bind(childNode, bindingContext);
					++noProblems;
        }
        catch(ProblemsWithChildrenError pwc)
        {
        }
        catch(SyntaxError se)
        {
        	bindingContext.addError(se);
					children[i] = new ErrorBoundNode(childNode);
        }
        catch (Throwable t)
        {
					bindingContext.addError(new BoundError(childNode, null, t));
					children[i] = new ErrorBoundNode(childNode);
        }
			}
			
			
    }
    
    if (noProblems != n)
    {
    	throw new ProblemsWithChildrenError(parentNode);
    }
		
		return children;
	}
	
	static protected IBoundNode[] bindTypeChildren(ISyntaxNode parentNode, IBindingContext bindingContext, IOpenClass type)
	{
		return bindTypeChildren(parentNode, bindingContext, type, 0, parentNode.getNumberOfChildren());	
	}


	static protected IBoundNode[] bindTypeChildren(ISyntaxNode parentNode, IBindingContext bindingContext, IOpenClass type, int from , int to)
	{
		int n = to - from;
		
		if (n == 0)
			return IBoundNode.EMPTY;
		
		IBoundNode[] children = new IBoundNode[n];  
		
		for (int i = 0; i < n; i++)
		{
			ISyntaxNode childNode = parentNode.getChild(from + i);
			if (childNode == null)
			{
				continue;
			} 
			INodeBinder binder = bindingContext.findBinder(childNode);
			if (binder == null)
			{
				bindingContext.addError(new BoundError(childNode, "Can not find binder for node:" + childNode.getType(), null));
				children[i] = new ErrorBoundNode(childNode);
			}
			else
			{
				try
				{
					children[i] = binder.bindType(childNode, bindingContext, type);
				}
				catch(SyntaxError se)
				{
					bindingContext.addError(se);
					children[i] = new ErrorBoundNode(childNode);
				}
				catch (Throwable t)
				{
					bindingContext.addError(new BoundError(childNode, null, t));
					children[i] = new ErrorBoundNode(childNode);
				}
			}
		}
		
		return children;
	}



	
	static public IBoundNode bindChildNode(ISyntaxNode node, IBindingContext bindingContext) 
	  throws Exception
	{
		
		INodeBinder binder = bindingContext.findBinder(node);
		if (binder == null)
		{
			throw new BoundError(node, "Can not find binder for node: " + node.getType(), null);
		}
		try
		{
			return binder.bind(node, bindingContext);
		}
		catch(SyntaxError se)
		{
			throw se;
		}
		catch (Throwable t)
		{
			throw new BoundError(node, null, t);
		}
	}
	
	static public IBoundNode bindTypeNode(ISyntaxNode node, IBindingContext bindingContext, IOpenClass type)
	  throws Exception
	{
		
		INodeBinder binder = bindingContext.findBinder(node);
		if (binder == null)
		{
			bindingContext.addError(new BoundError(node, "Can not bind node:" + node.getType(), null));
			return new ErrorBoundNode(node);
		}
		try
		{
			return binder.bindType(node, bindingContext, type);
        
		}
		catch(SyntaxError se)
		{
			throw se;
		}
		catch (Throwable t)
		{
			throw new BoundError(node, null, t);
		}
	}
	
	
	
	
	static public IBoundNode bindTargetNode(ISyntaxNode node, IBindingContext bindingContext, IBoundNode targetNode)
	  throws Exception
	{
		
		INodeBinder binder = bindingContext.findBinder(node);
		if (binder == null)
		{
			bindingContext.addError(new BoundError(node, "Can not bind node:" + node.getType(), null));
			return new ErrorBoundNode(node);
		}
		try
		{
			return binder.bindTarget(node, bindingContext, targetNode);
        
		}
		catch(SyntaxError se)
		{
			throw se;
		}
		catch (Throwable t)
		{
			throw new BoundError(node, null, t);
		}
	}


	
	static protected IOpenClass[] getTypes(IBoundNode[] nodes)
	{
		IOpenClass[] types = new IOpenClass[nodes.length];
		
		for (int i = 0; i < types.length; i++)
    {
      types[i] = nodes[i].getType();
    }
    return types; 
	}
	

	static public IOpenCast getCast(IBoundNode bnode, IOpenClass to, IBindingContext bindingContext)
	  throws Exception
	{
		
		IOpenClass from = bnode.getType();
		
		if (from == null)
			throw new TypeCastError(bnode.getSyntaxNode(), NullOpenClass.the, to);
			
		if(from.equals(to))
		  return null;

		IOpenCast	cast = bindingContext.getCast(from, to);
		
		if (cast == null || !cast.isImplicit())
		{
				throw new TypeCastError(bnode.getSyntaxNode(), from, to);
		}	
		
		return cast;		
		
	}	
	
	
	
  /* (non-Javadoc)
   * @see org.openl.binding.INodeBinder#bindTarget(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext, org.openl.types.IOpenClass)
   */
  public IBoundNode bindTarget(
    ISyntaxNode node,
    IBindingContext bindingContext,
    IBoundNode targetNode)
    throws Exception
  {
		throw new UnsupportedOperationException("This node does not support target binding");
  }

  /* (non-Javadoc)
   * @see org.openl.binding.INodeBinder#bindType(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext, org.openl.types.IOpenClass)
   */
  public IBoundNode bindType(
    ISyntaxNode node,
    IBindingContext bindingContext,
    IOpenClass type)
    throws Exception
  {
    IBoundNode bnode = bindChildNode(node, bindingContext);
    
    return convertType(bnode, bindingContext, type);
      
  }


  public static IBoundNode convertType(
      IBoundNode bnode,
      IBindingContext bindingContext,
      IOpenClass type)
      throws Exception
    {
  		
  		IOpenCast cast = getCast(bnode, type, bindingContext);
  		
  		if (cast == null)
  		  return bnode;
      
      return new CastNode(null,bnode, cast, type);
    }
  
  
	static public String getIdentifier(ISyntaxNode node)
	{
		return ((IdentifierNode)node).getIdentifier();
	}	


}
