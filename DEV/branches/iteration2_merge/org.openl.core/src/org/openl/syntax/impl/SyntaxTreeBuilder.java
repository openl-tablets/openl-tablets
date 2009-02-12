/*
 * Created on May 9, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.syntax.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.openl.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.text.TextInterval;

/**
 * @author snshor
 */
public class SyntaxTreeBuilder implements ISyntaxConstants
{
	
	List<ISyntaxError> parseErrors;
	
	IOpenSourceCodeModule module;
	
	
	public void uop(String type, TextInterval pos) 
	{
		ISyntaxNode left = pop();

		push( new UnaryNode(type, pos,  left, module) );
	}


	public void bop(String type, TextInterval pos) 
	{
		ISyntaxNode right = pop();
		ISyntaxNode left = pop();

		push( new BinaryNode(type, pos, left, right, module) );
	}

	public void identifier(String type, TextInterval pos, String image)
	{
		push( new IdentifierNode(type, pos, image, module));
	}
	
	public void literal(String type, TextInterval pos, String image)
	{
		push( new LiteralNode(type, pos,  image, module));
	}
	
	public void notImplemented(String type)
	{
			throw new RuntimeException(type + " has not been implemented yet");
	}
	
	public void emptyStatement(String type, TextInterval pos)
	{
		push(new EmptyNode(type, pos, module));
	}	
	


	
	
	public void nop(String type, TextInterval pos,  int args)
	{
		push(new NaryNode(type, pos, popN(args), module));
	}
	

	public void toMarker(String type, TextInterval pos, Object marker)
	{
		push(new NaryNode(type, pos, popToMarker(marker),  module));
	}
	


	public void nop(String type, TextInterval pos,  boolean[] args)
	{
		int n = args.length;
		
		ISyntaxNode[] nodes = new ISyntaxNode[n];
		
		for (int i = n - 1; i >= 0; --i)
    {
      nodes[i] = args[i] ? pop() : null; 
    }
		
		push(new NaryNode(type, pos, nodes,  module));
	}
	
	public void addError(SyntaxError exc)
	{
		if (parseErrors == null)
		  parseErrors = new ArrayList<ISyntaxError>();
		parseErrors.add(exc);
  }
  
  public ISyntaxError[] getSyntaxErrors()
  {
  	return parseErrors == null ? ISyntaxError.EMPTY : 
  	  (SyntaxError[])parseErrors.toArray(new SyntaxError[parseErrors.size()]); 
  }

	public ISyntaxNode getTopnode()
	{
		
		
		//TODO exception?
		switch(stack.size())
		{
			case 0:
//				addError(new SyntaxException());
				return null;
			case 1:
					return pop();	
			default:
				if (parseErrors != null && parseErrors.size() > 0)
					//it is OK to return, probably the application will check for errors			
			  	return pop();
			  	
			  //grammar problem???
			  ISyntaxNode node = pop();
			  addError(new SyntaxError(node, "More than one syntax node on stack:\nSource:\n" +  node.getModule().getCode()  , null));
			  return node;	
//			  throw new RuntimeException("More than one syntax node on stack");
		}
	}
	
	
		
	
	
////////////////////////////////////////////////////////////	
	
	Stack<Object> stack = new Stack<Object>();
	

	ISyntaxNode pop()
	{
		Object x = stack.pop();
		if (x instanceof ISyntaxNode)
			return (ISyntaxNode)x;
		return null;	
	}
	
	void push(ISyntaxNode sn)
	{
//		Log.debug("NODE: " + sn.getType());
		stack.push(sn);
	}
	
	protected ISyntaxNode[] popN(int n)
	{

		ISyntaxNode[] nodes = new ISyntaxNode[n];

		for(int i = 0; i < n; ++i)
		{
			nodes[n-1 - i]  = pop();
		}

		return nodes;
	}
	
	
	static class Marker {};
	
	public Object marker()
	{
		Object marker = new Marker();
		stack.push(marker);
		return marker;
	}
	
	
	public ISyntaxNode[] popToMarker(Object marker)
	{
		for(int i = 0, size = stack.size(); i < size; ++i)
		{
			if (stack.get(size - i - 1) == marker)
			{
				ISyntaxNode[] sn = popN(i);
				stack.pop(); //remove marker
				return sn; 
			}  
		}

		throw new RuntimeException("Marker not found");
	}
	

	
  /**
   * @return
   */
  public IOpenSourceCodeModule getModule()
  {
    return module;
  }

  /**
   * @param module
   */
  public void setModule(IOpenSourceCodeModule module)
  {
    this.module = module;
  }

}	
	
	

