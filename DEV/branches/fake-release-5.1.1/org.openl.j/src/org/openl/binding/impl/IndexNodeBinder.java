/*
 * Created on Jun 18, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;

/**
 * @author snshor
 * 
 */
public class IndexNodeBinder extends ANodeBinder
{

    static final public String INDEX_METHOD_NAME = "index";

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode,
     *      org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext)
	    throws Exception
    {
	throw new UnsupportedOperationException(
		"This node always binds  with target");
    }
    public IBoundNode bindTarget(ISyntaxNode node,
	    IBindingContext bindingContext, IBoundNode targetNode)
	    throws Exception
    {

	if (node.getNumberOfChildren() != 1)
	{
	    throw new BoundError(node,
		    "Index node must have  exactly 1 subnode");
	}

	IBoundNode[] children = bindChildren(node, bindingContext);

	IOpenClass indexExprType = children[0].getType();
	IOpenClass containerType = targetNode.getType();

	IOpenClass[] types = { containerType, indexExprType };

	IOpenIndex index = getMehodBasedIndex(types, bindingContext);

	if (index != null)
	    return new IndexNode(node, children, targetNode, index);

	IAggregateInfo info = containerType.getAggregateInfo();

	if (info != null
		&& (index = info.getIndex(containerType, indexExprType)) != null)
	{
	    return new IndexNode(node, children, targetNode, index);
	}

	throw new BoundError(node, "Index operator " + targetNode.getType() + "["
		+ indexExprType.getName() + "] not found");

    }
IOpenIndex getMehodBasedIndex(IOpenClass indexExprType,
	    IOpenClass componentType, IBindingContext cxt)
    {

	IMethodCaller reader = MethodSearch.getMethodCaller(INDEX_METHOD_NAME,
		new IOpenClass[] { indexExprType }, cxt, componentType);

	if (reader == null)
	    return null;

	IOpenClass returnType = reader.getMethod().getType();

	IMethodCaller writer = MethodSearch.getMethodCaller(INDEX_METHOD_NAME,
		new IOpenClass[] { indexExprType, returnType }, cxt,
		componentType);

	return new MethodBasedIndex(reader, writer);
    }

    IOpenIndex getMehodBasedIndex(IOpenClass[] types,
	    IBindingContext bindingContext)
    {

	IMethodCaller reader = BinaryOperatorNodeBinder
		.findBinaryOperatorMethodCaller(INDEX_METHOD_NAME, types,
			bindingContext);

	if (reader == null)
	{
	    IOpenClass[] params = { types[1] };

	    reader = MethodSearch.getMethodCaller(INDEX_METHOD_NAME, params,
		    bindingContext, types[0]);
	}

	if (reader == null)
	    return null;

	IOpenClass returnType = reader.getMethod().getType();

	IMethodCaller writer = bindingContext.findMethodCaller(
		"org.openl.operators", INDEX_METHOD_NAME, new IOpenClass[] {
			types[0], types[1], returnType });

	if (writer == null)
	{
	    IOpenClass[] params = { types[1], returnType };

	    writer = MethodSearch.getMethodCaller(INDEX_METHOD_NAME, params,
		    bindingContext, types[0]);
	}

	return new MethodBasedIndex(reader, writer);
    }

    static class MethodBasedIndex implements IOpenIndex
    {

	IMethodCaller reader;
	IMethodCaller writer;

	MethodBasedIndex(IMethodCaller reader, IMethodCaller writer)
	{
	    this.reader = reader;
	    this.writer = writer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.types.IOpenIndex#getElementType()
	 */
	public IOpenClass getElementType()
	{
	    return reader.getMethod().getType();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.types.IOpenIndex#getValue(java.lang.Object,
	 *      java.lang.Object)
	 */
	public Object getValue(Object container, Object index)
	{
	    int n = reader.getMethod().getSignature().getParameterTypes().length;

	    if (n == 2)
		return reader.invoke(null, new Object[] { container, index },
			null);
	    else
		return reader.invoke(container, new Object[] { index }, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.types.IOpenIndex#isWritable()
	 */
	public boolean isWritable()
	{
	    return writer != null;
	}

	public void setValue(Object container, Object index, Object value)
	{
	    int n = writer.getMethod().getSignature().getParameterTypes().length;

	    if (n == 3)
		writer.invoke(null, new Object[] { container, index, value },
			null);
	    else
		writer.invoke(container, new Object[] { index, value }, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.types.IOpenIndex#getIndexType()
	 */
	public IOpenClass getIndexType()
	{
	    int n = writer.getMethod().getSignature().getParameterTypes().length;
	    return reader.getMethod().getSignature().getParameterTypes()[n - 1];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.types.IOpenIndex#makeArrayInstance(int)
	 */
	public Object makeArrayInstance(int length)
	{
	    throw new UnsupportedOperationException();
	}

	public Object makeArrayInstance(int[] length)
	{
	    throw new UnsupportedOperationException();
	}
    }

}
