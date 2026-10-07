package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.util.ClassUtils;

/**
 * Binds the deprecated {@code ++} and {@code --} operators, written before or after a variable or a field.
 *
 * <p>The operator changes its operand by one and keeps the operand type. The prefix form gives the changed value, the
 * postfix form gives the value before the change.
 *
 * <p>Every use is reported as a deprecation warning that suggests {@code x += 1} or {@code x -= 1} instead.
 *
 * @author Yury Molchan
 */
public class IncrementNodeBinder extends ANodeBinder {

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) {
        var nodeType = node.getType();
        var methodName = nodeType.substring(nodeType.lastIndexOf('.') + 1);
        var children = bindChildren(node, bindingContext);
        var operand = children[0];
        if (!operand.isLvalue()) {
            return makeErrorNode("The node is not an Lvalue", operand.getSyntaxNode(), bindingContext);
        }

        var types = getTypes(children);
        var methodCaller = UnaryOperatorNodeBinder.findUnaryOperatorMethodCaller(methodName, types, bindingContext);
        if (methodCaller == null) {
            return makeErrorNode(UnaryOperatorNodeBinder.errorMsg(methodName, types[0]), node, bindingContext);
        }

        var operator = "inc".equals(methodName) ? "++" : "--";
        var resultClass = methodCaller.getMethod().getType().getInstanceClass();
        if (ClassUtils.primitiveToWrapper(resultClass) != ClassUtils.primitiveToWrapper(types[0].getInstanceClass())) {
            return makeErrorNode("Operator '%s' must return the same type as its operand.".formatted(operator),
                    node,
                    bindingContext);
        }

        BindHelper.processWarn("DEPRECATED '%s' operator will be removed in the next version. Use 'x %s= 1' instead."
                .formatted(operator, operator.charAt(0)), node, bindingContext);
        return new IncrementNode(node, operand, methodCaller, nodeType.startsWith("op.suffix."));
    }
}
