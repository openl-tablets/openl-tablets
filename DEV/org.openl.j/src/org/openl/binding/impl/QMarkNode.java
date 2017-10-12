package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * It supports 3 state in condition: false, null, true. The first branch is executed only when the condition is true.
 * 
 * @author Yury Molchan
 */
public class QMarkNode extends ABoundNode {

    private IOpenClass type;

    QMarkNode(ISyntaxNode syntaxNode, IBoundNode[] children, IOpenClass type) {
        super(syntaxNode, children);
        this.type = type;
    }

    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        Object res = children[0].evaluate(env);

        // To handle null in condition
        // (condition) ? TrueResult : NullOrFalseResult
        return (Boolean.TRUE.equals(res)) ? children[1].evaluate(env) : children[2].evaluate(env);
    }

    public IOpenClass getType() {
        return type;
    }

    @Override
    public boolean isLiteralExpressionParent() {
        return true;
    }

}
