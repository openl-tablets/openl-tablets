package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.vm.IRuntimeEnv;

/**
 * @author Yury Molchan
 */
public class LoopNode extends ABoundNode {

    private final IBoundNode initNode;
    private final IBoundNode conditionNode;
    private final IBoundNode blockCodeNode;
    private final IBoundNode afterNode;

    LoopNode(ISyntaxNode syntaxNode, IBoundNode conditionNode, IBoundNode blockCodeNode) {
        super(syntaxNode, conditionNode, blockCodeNode);
        this.initNode = null;
        this.conditionNode = conditionNode;
        this.blockCodeNode = blockCodeNode;
        this.afterNode = null;
    }

    LoopNode(ISyntaxNode syntaxNode,
            IBoundNode initNode,
            IBoundNode conditionNode,
            IBoundNode blockCodeNode,
            IBoundNode afterNode) {
        super(syntaxNode, initNode, conditionNode, blockCodeNode, afterNode);
        this.initNode = initNode;
        this.conditionNode = conditionNode;
        this.blockCodeNode = blockCodeNode;
        this.afterNode = afterNode;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        if (initNode != null) {
            initNode.evaluate(env);
        }
        // To support null values
        while (conditionNode == null || Boolean.TRUE.equals(conditionNode.evaluate(env))) {
            blockCodeNode.evaluate(env);
            if (afterNode != null) {
                afterNode.evaluate(env);
            }
        }
        return null;
    }
}
