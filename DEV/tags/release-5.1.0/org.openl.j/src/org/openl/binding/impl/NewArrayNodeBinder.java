/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.binding.impl;

import java.util.Vector;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 */

public class NewArrayNodeBinder extends ANodeBinder
{
	
	

  /* (non-Javadoc)
   * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv, org.openl.binding.IBindingContext)
   */
  public IBoundNode bind(
    ISyntaxNode node,
    IBindingContext bindingContext)
    throws Exception
  {
    
			int nc = node.getNumberOfChildren();
			if (nc != 1)
			{
    		throw new BoundError(node, "New array node must have 1 subnode");	
			}
			

			ISyntaxNode indexChild =  node.getChild(0);			

			int dim = 0;
			while(indexChild.getType().equals("array.index.empty"))
			{
				dim++;
				indexChild = indexChild.getChild(0);
			}
			
			Vector expressions = new Vector();
			
			while(indexChild.getType().equals("array.index.expression"))
			{
				expressions.add(indexChild.getChild(1));
				indexChild = indexChild.getChild(0);
			}
			
			int exprsize = expressions.size();
			
			IBoundNode[] exprAry = new IBoundNode[exprsize];
			
			
			for (int i = 0; i < exprAry.length; i++)
      {
        exprAry[i] = bindTypeNode((ISyntaxNode)expressions.elementAt(i), bindingContext, JavaOpenClass.INT);
      }
			
			
			ISyntaxNode typeNode =  indexChild;			
			
			String typeName = ((IdentifierNode)typeNode).getIdentifier();
			
			IOpenClass componentType = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeName);
			
			if (componentType == null)
			{
				throw new BoundError(typeNode, "Type " + typeName + " not found");
			}

		
			IAggregateInfo info = componentType.getAggregateInfo();
		
			IOpenClass arrayType = info.getIndexedAggregateType(componentType, dim + exprsize);				
    
			return new ArrayBoundNode(node, exprAry, dim, arrayType, componentType);
      
    
  }
  
}
