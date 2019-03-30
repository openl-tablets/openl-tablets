/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;

/**
 * @author snshor
 */

public class SuffixOperatorNodeBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv,
     * org.openl.binding.IBindingContext)
     */
    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        if (node.getNumberOfChildren() != 1) {
            return makeErrorNode("Suffix node should have 1 subnode", node, bindingContext);
        }

        int index = node.getType().lastIndexOf('.');
        String methodName = node.getType().substring(index + 1);
        IBoundNode[] children = bindChildren(node, bindingContext);

        if (!children[0].isLvalue()) {
            return makeErrorNode("The node is not an Lvalue", children[0].getSyntaxNode(), bindingContext);
        }

        IOpenClass[] types = getTypes(children);
        IMethodCaller methodCaller = UnaryOperatorNodeBinder.findUnaryOperatorMethodCaller(methodName,
            types,
            bindingContext);

        if (methodCaller == null) {
            String message = UnaryOperatorNodeBinder.errorMsg(methodName, types[0]);
            return makeErrorNode(message, node, bindingContext);
        }

        IOpenClass methodType = methodCaller.getMethod().getType();

        if (ClassUtils.primitiveToWrapper(methodType.getInstanceClass()) != ClassUtils.primitiveToWrapper(types[0].getInstanceClass())) {
            return makeErrorNode("Suffix operator must return the same type as an argument", node, bindingContext);
        }

        return new SuffixNode(node, children, methodCaller);
    }

}
