package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.cast.IOpenCast;
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
    private final ILocalVar tempVar;
    private final IBoundNode transformer;
    private final IBoundNode targetNode;
    private final IOpenCast openCast;
    private final boolean unique;

    TransformIndexNode(ISyntaxNode syntaxNode,
                       IBoundNode targetNode,
                       IBoundNode transformer,
                       ILocalVar tempVar,
                       IOpenCast openCast,
                       boolean unique) {
        super(syntaxNode, targetNode, transformer);
        this.tempVar = tempVar;
        this.targetNode = targetNode;
        this.transformer = transformer;
        this.openCast = openCast;
        this.unique = unique;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        var target = targetNode.evaluate(env);
        if (target == null) {
            return null;
        }
        var elementsIterator = targetNode.getType().getAggregateInfo().getIterator(target);
        Collection<Object> result = unique ? new LinkedHashSet<>() : new ArrayList<>();
        while (elementsIterator.hasNext()) {
            var element = elementsIterator.next();
            if (element == null) {
                continue;
            }
            element = openCast != null ? openCast.convert(element) : element;
            tempVar.set(null, element, env);
            var transformed = transformer.evaluate(env);
            if (!unique || transformed != null) {
                result.add(transformed);
            }
        }
        return CollectionUtils.toArray(result, transformer.getType().getInstanceClass());
    }

    @Override
    public IOpenClass getType() {
        var componentType = transformer.getType();
        return componentType.getAggregateInfo().getIndexedAggregateType(componentType);
    }
}
