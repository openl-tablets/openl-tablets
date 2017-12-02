package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class WhileNode extends ABoundNode {

    /**
     * @param syntaxNode
     * @param children
     */
    public WhileNode(ISyntaxNode syntaxNode, IBoundNode[] children) {
        super(syntaxNode, children);
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        if (children[0] != null) {
            children[0].evaluate(env);
        }

        while (true) {
            // check condition
            Boolean b = (Boolean) children[0].evaluate(env);
            if (!b.booleanValue()) {
                break;
            }

            children[1].evaluate(env);

        }

        return null;
    }

    public IOpenClass getType() {
        return NullOpenClass.the;
    }

}
