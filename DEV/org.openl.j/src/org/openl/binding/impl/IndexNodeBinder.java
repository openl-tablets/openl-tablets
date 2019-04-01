/*
 * Created on Jun 18, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IAggregateInfo;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;

/**
 * @author snshor
 *
 */
public class IndexNodeBinder extends ANodeBinder {

    public static final String INDEX_METHOD_NAME = "index";

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.syntax.ISyntaxNode, org.openl.binding.IBindingContext)
     */
    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        return makeErrorNode("This node always binds  with target", node, bindingContext);
    }

    @Override
    public IBoundNode bindTarget(ISyntaxNode node,
            IBindingContext bindingContext,
            IBoundNode targetNode) throws Exception {

        if (node.getNumberOfChildren() != 1) {
            return makeErrorNode("Index node must have  exactly 1 subnode", node, bindingContext);
        }

        IBoundNode[] children = bindChildren(node, bindingContext);

        IOpenClass indexExprType = children[0].getType();
        IOpenClass containerType = targetNode.getType();

        IOpenClass[] types = { containerType, indexExprType };
        IOpenIndex index = getMethodBasedIndex(types, bindingContext);

        if (index != null) {
            return new IndexNode(node, children, targetNode, index);
        }

        IAggregateInfo info = containerType.getAggregateInfo();

        if (info != null && (index = info.getIndex(containerType, indexExprType)) != null) {
            return new IndexNode(node, children, targetNode, index);
        }

        String message = String
            .format("Index operator %s[%s] is not found", targetNode.getType(), indexExprType.getName());
        return makeErrorNode(message, node, bindingContext);
    }

    private IOpenIndex getMethodBasedIndex(IOpenClass[] types, IBindingContext bindingContext) {

        IMethodCaller reader = BinaryOperatorNodeBinder
            .findBinaryOperatorMethodCaller(INDEX_METHOD_NAME, types, bindingContext);

        if (reader == null) {
            IOpenClass[] params = { types[1] };
            reader = MethodSearch.findMethod(INDEX_METHOD_NAME, params, bindingContext, types[0]);
        }

        if (reader == null) {
            return null;
        }

        IOpenClass returnType = reader.getMethod().getType();

        IMethodCaller writer = bindingContext.findMethodCaller(ISyntaxConstants.OPERATORS_NAMESPACE,
            INDEX_METHOD_NAME,
            new IOpenClass[] { types[0], types[1], returnType });

        if (writer == null) {
            IOpenClass[] params = { types[1], returnType };
            writer = MethodSearch.findMethod(INDEX_METHOD_NAME, params, bindingContext, types[0]);
        }

        return new MethodBasedIndex(reader, writer);
    }

}
