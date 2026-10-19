package org.openl.binding.impl;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.util.BooleanUtils;
import org.openl.vm.IRuntimeEnv;

class SelectFirstIndexNode extends ABoundNode {
    private final ElementVariables variables;
    private final IBoundNode condition;
    private final IBoundNode targetNode;

    SelectFirstIndexNode(ISyntaxNode syntaxNode,
                         IBoundNode targetNode,
                         IBoundNode condition,
                         ElementVariables variables) {
        super(syntaxNode, variables.children(targetNode, condition));
        this.variables = variables;
        this.targetNode = targetNode;
        this.condition = condition;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        var target = targetNode.evaluate(env);
        if (target == null) {
            return null;
        }
        return variables.findFirst(targetNode.getType().getAggregateInfo().getIterator(target),
                env,
                (element, value) -> BooleanUtils.toBoolean(condition.evaluate(env)));
    }

    @Override
    public IOpenClass getType() {
        var type = targetNode.getType();
        return type.getAggregateInfo().getComponentType(type);
    }
}
