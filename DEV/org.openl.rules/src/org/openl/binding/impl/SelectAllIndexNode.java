package org.openl.binding.impl;

import java.util.ArrayList;

import lombok.Getter;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.util.BooleanUtils;
import org.openl.util.CollectionUtils;
import org.openl.vm.IRuntimeEnv;

class SelectAllIndexNode extends ABoundNode {
    private final ElementVariables variables;
    private final IBoundNode condition;
    private final IBoundNode targetNode;
    private final Class<?> componentClass;
    @Getter
    private final IOpenClass type;

    SelectAllIndexNode(ISyntaxNode syntaxNode,
                       IBoundNode targetNode,
                       IBoundNode condition,
                       ElementVariables variables) {
        super(syntaxNode, variables.children(targetNode, condition));
        this.variables = variables;
        this.targetNode = targetNode;
        this.condition = condition;

        if (targetNode.getType().isArray()) {
            this.componentClass = targetNode.getType().getComponentClass().getInstanceClass();
            this.type = targetNode.getType();
        } else {
            // Collection
            var componentType = variables.element().getType();
            this.componentClass = componentType.getInstanceClass();
            this.type = componentType.getAggregateInfo().getIndexedAggregateType(componentType);
        }
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        var target = targetNode.evaluate(env);
        if (target == null) {
            return null;
        }
        var firedElements = new ArrayList<Object>();
        variables.forEach(targetNode.getType().getAggregateInfo().getIterator(target), env, (element, value) -> {
            if (BooleanUtils.toBoolean(condition.evaluate(env))) {
                firedElements.add(element);
            }
        });
        return CollectionUtils.toArray(firedElements, componentClass);
    }
}
