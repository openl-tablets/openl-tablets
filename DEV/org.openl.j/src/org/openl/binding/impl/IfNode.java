package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * It supports 3 state in condition: false, null, true. The first branch is executed only when the condition is true.
 *
 * @author Yury Molchan
 */
public class IfNode extends ABoundNode {

    private final IBoundNode conditionNode;
    private final IBoundNode elseNode;
    private final IBoundNode thenNode;
    private final IOpenClass type;

    IfNode(ISyntaxNode syntaxNode,
            IBoundNode conditionNode,
            IBoundNode thenNode,
            IOpenClass type) {
        super(syntaxNode, conditionNode, thenNode);
        this.conditionNode = conditionNode;
        this.thenNode = thenNode;
        this.elseNode = null;
        this.type = type;
    }

    IfNode(ISyntaxNode syntaxNode,
           IBoundNode conditionNode,
           IBoundNode thenNode,
           IBoundNode elseNode,
           IOpenClass type) {
        super(syntaxNode, conditionNode, thenNode, elseNode);
        this.conditionNode = conditionNode;
        this.thenNode = thenNode;
        this.elseNode = elseNode;
        this.type = type;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {

        Object res = conditionNode.evaluate(env);

        // if (condition) { TrueBranch } else { NullOrFalseBranch }
        return (Boolean.TRUE.equals(res)) ? thenNode.evaluate(env) : (elseNode != null ? elseNode.evaluate(env) : null);
    }

    @Override
    public IOpenClass getType() {
        return type;
    }

}
