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
import org.openl.types.IOpenClass;

/**
 * @author snshor
 */

public class BinaryOperatorOrNodeBinder extends BinaryOperatorNodeBinder
{

	/* (non-Javadoc)
	 * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv, org.openl.binding.IBindingContext)
	 */
	public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext)
		throws Exception
	{

		if (node.getNumberOfChildren() != 2)
		{
			throw new BoundError(
				node,
				"Binary node must have 2 subnodes",
				null);
		}

		int index = node.getType().lastIndexOf('.');

		String methodName = node.getType().substring(index + 1);

		IBoundNode[] children = bindChildren(node, bindingContext);

		IOpenClass[] types = getTypes(children);

		IMethodCaller om =
			findBinaryOperatorMethodCaller(methodName, types, bindingContext);

		if (om == null)
			throw new BoundError(
				node,
				errorMsg(methodName, types[0], types[1]));

		return new BinaryOpNodeOr(node, children, om);

	}



}
