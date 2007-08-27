/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.binding.impl;

import org.openl.binding.FieldNotFoundException;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenField;

/**
 * @author snshor
 */

public class IdentifierBinder extends ANodeBinder
{
	
	

  /* (non-Javadoc)
   * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv, org.openl.binding.IBindingContext)
   */
  public IBoundNode bind(
    ISyntaxNode node,
    IBindingContext bindingContext) throws Exception
  {
    
			String fieldName = ((IdentifierNode)node).getIdentifier();   

		
			IOpenField om =  bindingContext.findVar(ISyntaxConstants.THIS_NAMESPACE, fieldName);
			
			if (om ==  null)
			  throw new BoundError(node, "Field not found: " + fieldName,  null); 
    
			return new FieldBoundNode(node,  om);
      
    
  }

	public IBoundNode bindTarget(
		ISyntaxNode node,
		IBindingContext bindingContext,
		IBoundNode target)
	{
    
		try
		{
			
			
			
			String fieldName = ((IdentifierNode)node).getIdentifier();   

		
			IOpenField of =  target.getType().getField( fieldName);
			
			if (of ==  null)
				throw new FieldNotFoundException("Identifier: ", fieldName); 
    
			return new FieldBoundNode(node,  of, target);
      
		}
		catch (Throwable t)
		{
			bindingContext.addError(new BoundError(node, "Identifier:", t));
			return new ErrorBoundNode(node);
		}
    
	}




}
