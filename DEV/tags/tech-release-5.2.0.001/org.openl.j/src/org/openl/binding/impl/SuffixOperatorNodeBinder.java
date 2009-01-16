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

public class SuffixOperatorNodeBinder extends ANodeBinder
{

	/* (non-Javadoc)
	 * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv, org.openl.binding.IBindingContext)
	 */
	public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext)
		throws Exception
	{

		if (node.getNumberOfChildren() != 1)
		{
			throw new BoundError(node, "Suffix node should have 1 subnode");
		}

		int index = node.getType().lastIndexOf('.');

		String methodName = node.getType().substring(index + 1);

		IBoundNode[] children = bindChildren(node, bindingContext);

		if (!children[0].isLvalue())
		{
			throw new BoundError(
				children[0].getSyntaxNode(),
				"The node is not an Lvalue");
		}

		IOpenClass[] types = getTypes(children);

		IMethodCaller om =
			UnaryOperatorNodeBinder.findUnaryOperatorMethodCaller(
				methodName,
				types,
				bindingContext);

		if (om == null)
			throw new BoundError(
				node,
				UnaryOperatorNodeBinder.errorMsg(methodName, types[0]));

		if (!om.getMethod().getType().equals(types[0]))
		{
			throw new BoundError(
				node,
				"Suffix operator must return the same type as an argument");
		}

		return new SuffixNode(node, children, om);

	}

}
