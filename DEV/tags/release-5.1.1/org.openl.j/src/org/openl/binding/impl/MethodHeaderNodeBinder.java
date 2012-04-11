package org.openl.binding.impl;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.module.MethodParametersNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.impl.OpenMethodHeader;

/*
 * Created on Sep 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

/**
 * @author snshor
 *
 */
public class MethodHeaderNodeBinder extends ANodeBinder
{

	static final int TYPE_NODE = 0,
		METHOD_NAME_NODE = 1,
		PARAMETERS_NODE = 2,
		BODY_NODE = 3;



  /* (non-Javadoc)
   * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext)
   */
  public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext)
    throws Exception
  {
		IBoundNode typeNode =
			bindChildNode(node.getChild(TYPE_NODE), bindingContext);
      
    
//		methodBindingContext.setReturnType(typeNode.getType());
      
		String methodName =
			((IdentifierNode)node.getChild(METHOD_NAME_NODE)).getIdentifier();


		ISyntaxNode parametersNode = node.getChild(PARAMETERS_NODE);
		
		MethodParametersNode boundParametersNode = (MethodParametersNode)bindChildNode(parametersNode, bindingContext);

		OpenMethodHeader header = new OpenMethodHeader(methodName, typeNode.getType(), boundParametersNode.getSignature(), null);

		return new MethodHeaderNode(node ,header);
  }
  

}
