/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 */

public class AssignOperatorNodeBinder extends ANodeBinder {

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv,
     * org.openl.binding.IBindingContext)
     */
    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) {
        return bind(node, bindingContext, bindingContext);
    }

    /**
     * Binds an assignment whose target and value are resolved in different binding contexts.
     *
     * <p>A named argument of a constructor, as in {@code new Customer(name = name)}, assigns a field of the new
     * object. Its target is a field of that object. Its value is an expression of the caller: {@code name} on the
     * right is the parameter or variable of the caller, not the field of the new object.
     *
     * @param targetContext  the context the target is resolved in
     * @param bindingContext the context the value is resolved in, which also receives the errors
     */
    static IBoundNode bind(ISyntaxNode node, IBindingContext targetContext, IBindingContext bindingContext) {

        if (node.getNumberOfChildren() != 2) {
            return makeErrorNode("Expected two child nodes in assign node.", node, bindingContext);
        }

        var index = node.getType().lastIndexOf('.');
        var methodName = node.getType().substring(index + 1);

        var target = bindChildNode(node.getChild(0), targetContext);
        var source = bindChildNode(node.getChild(1), bindingContext);
        if (!target.isLvalue()) {
            return makeErrorNode("Impossible to assign value.", node, bindingContext);
        }

        var targetType = target.getType();
        var sourceType = source.getType();
        IMethodCaller methodCaller = null;

        if (!"assign".equals(methodName)) {

            methodCaller = BinaryOperatorNodeBinder.findBinaryOperatorMethodCaller(methodName,
                    new IOpenClass[]{targetType, sourceType},
                    bindingContext);

            if (methodCaller == null) {

                String message = BinaryOperatorNodeBinder.errorMsg(methodName, targetType, sourceType);
                return makeErrorNode(message, node, bindingContext);
            }
        }

        if (target.getDims() > 0) {
            return makeErrorNode("Multi-reference assignment is not supported.", node, bindingContext);
        }

        IOpenClass rightType = methodCaller == null ? sourceType : methodCaller.getMethod().getType();
        IOpenCast cast = null;

        if (!rightType.equals(targetType)) {

            cast = bindingContext.getCast(rightType, targetType);

            // only implicit casts and explicit casts for literal are allowed for right part
            if (cast == null || (!cast.isImplicit() && !(source instanceof LiteralBoundNode))) {
                var message = "Cannot convert from '%s' to '%s'."
                        .formatted(rightType.getName(), targetType.getName());
                return makeErrorNode(message, node, bindingContext);
            }
        }

        // Validate literal values against domain type at compile time (only for plain assignment)
        if (methodCaller == null) {
            BindHelper.validateDomainValue(source, targetType, bindingContext);
        }

        /*
         * target = source - simple assign target += source - assign with operation through methodCaller
         */
        return new AssignNode(node, target, source, methodCaller, cast);
    }

}
