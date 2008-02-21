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

public class BinaryOperatorNodeBinder extends ANodeBinder
{

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode,
     *      org.openl.env.IOpenEnv, org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext)
	    throws Exception
    {

	if (node.getNumberOfChildren() != 2)
	{
	    throw new BoundError(node, "Binary node must have 2 subnodes", null);
	}

	int index = node.getType().lastIndexOf('.');

	String methodName = node.getType().substring(index + 1);

	IBoundNode[] children = bindChildren(node, bindingContext);

//	IOpenClass[] types = getTypes(children);
//
//	IMethodCaller om = findBinaryOperatorMethodCaller(methodName, types,
//		bindingContext);
//
//	if (om == null)
//	    throw new BoundError(node, errorMsg(methodName, types[0], types[1]));
//
//	return new BinaryOpNode(node, children, om);
	return bindOperator(node, methodName, children[0], children[1], bindingContext);

    }

    static public IBoundNode bindOperator(ISyntaxNode node,
	    String operatorName, IBoundNode b1, IBoundNode b2,
	    IBindingContext bindingContext) throws BoundError
    {
	IOpenClass[] types = { b1.getType(), b2.getType() };

	IMethodCaller om = findBinaryOperatorMethodCaller(operatorName, types,
		bindingContext);

	if (om == null)
	    throw new BoundError(node, errorMsg(operatorName, types[0],
		    types[1]));

	return new BinaryOpNode(node, new IBoundNode[] { b1, b2 }, om);
    }

    static public IMethodCaller findBinaryOperatorMethodCaller(
	    String methodName, IOpenClass[] types,
	    IBindingContext bindingContext)
    {

	IMethodCaller om = bindingContext.findMethodCaller(
		"org.openl.operators", methodName, types);

	if (om != null)
	    return om;

	IOpenClass[] types2 = { types[1] };

	om = MethodSearch.getMethodCaller(methodName, types2, bindingContext,
		types[0]);

	if (om != null)
	    return om;

	om = MethodSearch.getMethodCaller(methodName, types, bindingContext,
		types[0]);

	if (om != null)
	    return om;

	om = MethodSearch.getMethodCaller(methodName, types, bindingContext,
		types[1]);

	return om;

    }

    static public String errorMsg(String methodName, IOpenClass t1,
	    IOpenClass t2)
    {
	return "Operator not defined for: " + methodName + "(" + t1.getName()
		+ ", " + t2.getName() + ")";

    }

}
