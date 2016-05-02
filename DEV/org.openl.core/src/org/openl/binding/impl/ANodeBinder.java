/*
 * Created on May 20, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.INodeBinder;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;

/**
 * @author snshor
 * 
 */
public abstract class ANodeBinder implements INodeBinder {

    public static IBoundNode bindChildNode(ISyntaxNode node, IBindingContext bindingContext) {

        INodeBinder binder = findBinder(node, bindingContext);

        if (binder == null) {
            return new ErrorBoundNode(node);
        }

        try {
            return binder.bind(node, bindingContext);
        } catch (Throwable t) {
            BindHelper.processError(t, node, bindingContext, false);

            return new ErrorBoundNode(node);
        }
    }

    public static IBoundNode bindTargetNode(ISyntaxNode node, IBindingContext bindingContext, IBoundNode targetNode) {
    	
    	if (targetNode.getClass() == ErrorBoundNode.class)
            return new ErrorBoundNode(node);

        INodeBinder binder = findBinder(node, bindingContext);

        if (binder == null) {
            return new ErrorBoundNode(node);
        }

        try {
            return binder.bindTarget(node, bindingContext, targetNode);
        } catch (Throwable t) {
            BindHelper.processError(node, t, bindingContext);

            return new ErrorBoundNode(node);
        }
    }

    public static IBoundNode bindTypeNode(ISyntaxNode node, IBindingContext bindingContext, IOpenClass type) {

        INodeBinder binder = findBinder(node, bindingContext);

        if (binder == null) {
            return new ErrorBoundNode(node);
        }

        try {
            return binder.bindType(node, bindingContext, type);
        } catch (Throwable t) {
            BindHelper.processError(t, node, bindingContext, false);

            return new ErrorBoundNode(node);
        }
    }

    public static IBoundNode[] bindChildren(ISyntaxNode parentNode, IBindingContext bindingContext) throws SyntaxNodeException {

        return bindChildren(parentNode, bindingContext, 0, parentNode.getNumberOfChildren());
    }

    public static IBoundNode[] bindChildren(ISyntaxNode parentNode, IBindingContext bindingContext, int from, int to)
        throws SyntaxNodeException {

        int n = to - from;

        if (n == 0) {
            return new IBoundNode[0];
        }

        IBoundNode[] children = new IBoundNode[n];

        int boundNodesCount = 0;

        for (int i = 0; i < n; i++) {

            ISyntaxNode childNode = parentNode.getChild(from + i);

            if (childNode == null) {
                boundNodesCount += 1;
                continue;
            }

            children[i] = bindChildNode(childNode, bindingContext);
            boundNodesCount += 1;
        }

        if (boundNodesCount != n) {
            String message = "Can not bind node";
            BindHelper.processError(message, parentNode, bindingContext);

            ErrorBoundNode errorBoundNode = new ErrorBoundNode(parentNode);
            return new IBoundNode[] { errorBoundNode };
        }

        return children;
    }

    public static IBoundNode[] bindTypeChildren(ISyntaxNode parentNode, IBindingContext bindingContext, IOpenClass type) {
        return bindTypeChildren(parentNode, bindingContext, type, 0, parentNode.getNumberOfChildren());
    }

    public static IBoundNode[] bindTypeChildren(ISyntaxNode parentNode,
                                                IBindingContext bindingContext,
                                                IOpenClass type,
                                                int from,
                                                int to) {

        int n = to - from;

        if (n == 0) {
            return new IBoundNode[0];
        }

        IBoundNode[] children = new IBoundNode[n];

        for (int i = 0; i < n; i++) {

            ISyntaxNode childNode = parentNode.getChild(from + i);

            if (childNode == null) {
                continue;
            }

            children[i] = bindTypeNode(childNode, bindingContext, type);
        }

        return children;
    }

    public static IOpenClass[] getTypes(IBoundNode[] nodes) {

        IOpenClass[] types = new IOpenClass[nodes.length];

        for (int i = 0; i < types.length; i++) {
            types[i] = nodes[i].getType();
        }

        return types;
    }

    private static INodeBinder findBinder(ISyntaxNode node, IBindingContext bindingContext) {

        INodeBinder binder = bindingContext.findBinder(node);

        if (binder == null) {
            String message = String.format("Can not find binder for node type '%s'", node.getType());
            BindHelper.processError(message, node, bindingContext);
        }

        return binder;
    }

    private static IBoundNode convertType(IBoundNode node, IBindingContext bindingContext, IOpenClass type)
        throws Exception {

        IOpenCast cast = getCast(node, type, bindingContext);

        if (cast == null) {
            return node;
        }

        return new CastNode(null, node, cast, type);
    }

    public static IOpenCast getCast(IBoundNode node, IOpenClass to, IBindingContext bindingContext)
            throws TypeCastException {
        return getCast(node, to, bindingContext, true);
    }

    public static IOpenCast getCast(IBoundNode node, IOpenClass to, IBindingContext bindingContext, boolean implicitOnly)
            throws TypeCastException {
        IOpenClass from = node.getType();

        if (from == null) {
            throw new TypeCastException(node.getSyntaxNode(), NullOpenClass.the, to);
        }

        if (from.equals(to)) {
            return null;
        }

        IOpenCast cast = bindingContext.getCast(from, to);

        if (cast == null || (implicitOnly && !cast.isImplicit())) {
        	if (!NullOpenClass.isAnyNull(from, to))
        	{	
        		throw new TypeCastException(node.getSyntaxNode(), from, to);
        	}	
        }

        return cast;
    }

    public static String getIdentifier(ISyntaxNode node) {
        return ((IdentifierNode) node).getIdentifier();
    }

    /*
     * (non-Javadoc)
     * @see org.openl.binding.INodeBinder#bindTarget(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext,
     * org.openl.types.IOpenClass)
     */
    public IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode targetNode)
        throws Exception {

        BindHelper.processError("This node does not support target binding", node, bindingContext);

        return new ErrorBoundNode(node);
        //        throw new UnsupportedOperationException("This node does not support target binding");
    }

    /*
     * (non-Javadoc)
     * @see org.openl.binding.INodeBinder#bindType(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext,
     * org.openl.types.IOpenClass)
     */
    public IBoundNode bindType(ISyntaxNode node, IBindingContext bindingContext, IOpenClass type) throws Exception {

        IBoundNode boundNode = bindChildNode(node, bindingContext);

        return convertType(boundNode, bindingContext, type);
    }

    protected static IOpenClass[] replace(int index, IOpenClass[] oldArray, IOpenClass newValue) {
        IOpenClass[] newArray = new IOpenClass[oldArray.length];
        System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);
        newArray[index] = newValue;
        return newArray;
    }
}
