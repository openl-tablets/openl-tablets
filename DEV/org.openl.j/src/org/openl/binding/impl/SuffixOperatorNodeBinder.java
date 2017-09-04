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
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        if (node.getNumberOfChildren() != 1) {
            BindHelper.processError("Suffix node should have 1 subnode", node, bindingContext, false);

            return new ErrorBoundNode(node);
        }

        int index = node.getType().lastIndexOf('.');
        String methodName = node.getType().substring(index + 1);
        IBoundNode[] children = bindChildren(node, bindingContext);

        if (!children[0].isLvalue()) {
            BindHelper.processError("The node is not an Lvalue", children[0].getSyntaxNode(), bindingContext, false);

            return new ErrorBoundNode(node);
        }

        IOpenClass[] types = getTypes(children);
        IMethodCaller methodCaller = UnaryOperatorNodeBinder.findUnaryOperatorMethodCaller(methodName,
            types,
            bindingContext);

        if (methodCaller == null) {

            String message = UnaryOperatorNodeBinder.errorMsg(methodName, types[0]);
            BindHelper.processError(message, node, bindingContext, false);

            return new ErrorBoundNode(node);
        }

        IOpenClass methodType = methodCaller.getMethod().getType();

        if (ClassUtils.primitiveToWrapper(methodType.getInstanceClass()) != ClassUtils.primitiveToWrapper(types[0].getInstanceClass())) {
//        if (!methodCaller.getMethod().getType().equals(types[0])) {
            BindHelper.processError("Suffix operator must return the same type as an argument", node, bindingContext, false);

            return new ErrorBoundNode(node);
        }

        return new SuffixNode(node, children, methodCaller);
    }

}
