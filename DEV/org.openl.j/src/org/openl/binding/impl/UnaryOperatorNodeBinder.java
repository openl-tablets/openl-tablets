/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 */

public class UnaryOperatorNodeBinder extends ANodeBinder {

    public static String errorMsg(String methodName, IOpenClass t1) {
        return "Operator not defined: " + methodName + "(" + t1.getName() + ")";

    }

    public static IMethodCaller findUnaryOperatorMethodCaller(String methodName,
                                                              IOpenClass[] types,
                                                              IBindingContext bindingContext) {

        IMethodCaller methodCaller = bindingContext.findMethodCaller(ISyntaxConstants.OPERATORS_NAMESPACE, methodName, types);

        if (methodCaller != null) {
            return methodCaller;
        }

        methodCaller = MethodSearch.findMethod(methodName, IOpenClass.EMPTY, bindingContext, types[0]);

        if (methodCaller != null) {
            return methodCaller;
        }

        methodCaller = MethodSearch.findMethod(methodName, types, bindingContext, types[0]);

        return methodCaller;
    }

    /*
     * (non-Javadoc)
     * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv,
     * org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        if (node.getNumberOfChildren() != 1) {
            return makeErrorNode("Unary node should have 1 subnode", node, bindingContext);
        }

        int index = node.getType().lastIndexOf('.');
        String methodName = node.getType().substring(index + 1);

        IBoundNode[] children = bindChildren(node, bindingContext);
        IOpenClass[] types = getTypes(children);

        IMethodCaller methodCaller = findUnaryOperatorMethodCaller(methodName, types, bindingContext);

        if (methodCaller == null) {

            String message = errorMsg(methodName, types[0]);
            return makeErrorNode(message, node, bindingContext);
        }

        return new UnaryOpNode(node, children, methodCaller);
    }

}
