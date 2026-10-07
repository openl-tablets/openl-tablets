package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMethodCaller;
import org.openl.vm.IRuntimeEnv;

/**
 * The deprecated {@code ++} or {@code --} operator applied to a variable or a field.
 *
 * <p>It stores the changed value in the operand. The prefix form gives the changed value, the postfix form gives the
 * value the operand had before the change.
 *
 * @author Yury Molchan
 */
class IncrementNode extends MethodBoundNode {

    private final boolean postfix;

    IncrementNode(ISyntaxNode syntaxNode, IBoundNode operand, IMethodCaller method, boolean postfix) {
        super(syntaxNode, method, operand);
        this.postfix = postfix;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        var oldValue = children[0].evaluate(env);
        var newValue = boundMethod.invoke(null, new Object[]{oldValue}, env);
        children[0].assign(newValue, env);
        return postfix ? oldValue : newValue;
    }
}
