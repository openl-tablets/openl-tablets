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
public class IfNode extends ABoundNode {

    IfNode(ISyntaxNode syntaxNode, IBoundNode[] children) {
        super(syntaxNode, children);
    }

    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {

        Object cond = children[0].evaluate(env);

        // if (condition) { TrueBranch } else { NullOrFalseBranch }
        Object res;
        if (Boolean.TRUE.equals(cond)) {
            res = children[1].evaluate(env);
        } else if (children.length > 2) {
            res = children[2].evaluate(env);
        } else {
            res = null;
        }

        return res;
    }

    public IOpenClass getType() {
        // return NullOpenClass.the;
        // TODO use both branches, see QMarkNode
        // var = if (condition) { res1 } else { res2 }
        // var = (condition) ? res1 : res2

        return children[1].getType();
    }

    @Override
    public boolean isLiteralExpressionParent() {
        return true;
    }

}
