/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 */

public class AssignOperatorNodeBinder extends ANodeBinder
{

	/* (non-Javadoc)
	 * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv, org.openl.binding.IBindingContext)
	 */
	public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext)
		throws Exception
	{

		if (node.getNumberOfChildren() != 2)
		{
			throw new BoundError(node, "Assign node must have 2 subnodes");
		}

		int index = node.getType().lastIndexOf('.');

		String methodName = node.getType().substring(index + 1);

		IBoundNode[] children = bindChildren(node, bindingContext);

		if (!children[0].isLvalue())
		{
			throw new BoundError(
			  children[0].getSyntaxNode(), 
				"The node "
					+ children[0].getClass().getName()
					+ " is not an Lvalue");
		}

		IOpenClass[] types = getTypes(children);

		IOpenClass leftType = types[0];

		IMethodCaller om = null;

		if (!methodName.equals("assign"))
		{

			om = BinaryOperatorNodeBinder.findBinaryOperatorMethodCaller(methodName, types, bindingContext);

			if (om == null)
				throw new BoundError(
				  node,
				  BinaryOperatorNodeBinder.errorMsg(methodName, types[0], types[1])
        );

		}

		IOpenClass rightType = om == null ? types[1] : om.getMethod().getType();

		IOpenCast cast = null;
		if (!rightType.equals(leftType))
		{
			cast = bindingContext.getCast(rightType, leftType);
			if (cast == null || !cast.isImplicit())
				throw new BoundError(node,
					"Can not convert from "
						+ rightType.getName()
						+ " to "
						+ leftType.getName());
		}

		return new AssignNode(node, children, om, cast);

	}

}
