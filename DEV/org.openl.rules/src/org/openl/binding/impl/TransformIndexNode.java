package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.vm.IRuntimeEnv;

/**
 * Evaluates TRANSFORM TO and TRANSFORM UNIQUE TO: the values of the expression for the elements of an array or a
 * collection, in the order of the elements.
 * <p>
 * TRANSFORM UNIQUE TO keeps the first of equal values and drops a null value.
 */
class TransformIndexNode extends ABoundNode {
    private final ElementVariables variables;
    private final IBoundNode transformer;
    private final IBoundNode targetNode;
    private final boolean unique;

    TransformIndexNode(ISyntaxNode syntaxNode,
                       IBoundNode targetNode,
                       IBoundNode transformer,
                       ElementVariables variables,
                       boolean unique) {
        super(syntaxNode, variables.children(targetNode, transformer));
        this.variables = variables;
        this.targetNode = targetNode;
        this.transformer = transformer;
        this.unique = unique;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        var target = targetNode.evaluate(env);
        if (target == null) {
            return null;
        }
        Collection<Object> result = unique ? new LinkedHashSet<>() : new ArrayList<>();
        variables.forEach(targetNode.getType().getAggregateInfo().getIterator(target), env, (element, value) -> {
            var transformed = transformer.evaluate(env);
            if (!unique || transformed != null) {
                result.add(transformed);
            }
        });
        return CollectionUtils.toArray(result, transformer.getType().getInstanceClass());
    }

    @Override
    public IOpenClass getType() {
        var componentType = transformer.getType();
        return componentType.getAggregateInfo().getIndexedAggregateType(componentType);
    }
}
