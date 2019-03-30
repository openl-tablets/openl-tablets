package org.openl.binding.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaArrayAggregateInfo;
import org.openl.util.CollectionUtils;
import org.openl.vm.IRuntimeEnv;

class TransformToUniqueIndexNode extends ABoundNode {
    private ILocalVar tempVar;
    private IBoundNode transformer;
    private IBoundNode targetNode;
    private Class<?> componentClass;
    private IOpenClass resultType;

    TransformToUniqueIndexNode(ISyntaxNode syntaxNode,
            IBoundNode targetNode,
            IBoundNode transformer,
            ILocalVar tempVar) {
        super(syntaxNode, targetNode, transformer);
        this.tempVar = tempVar;
        this.targetNode = targetNode;
        this.transformer = transformer;
        IOpenClass componentType = transformer.getType();
        this.componentClass = componentType.getInstanceClass();
        this.resultType = JavaArrayAggregateInfo.ARRAY_AGGREGATE.getIndexedAggregateType(componentType);
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Iterator<Object> elementsIterator = targetNode.getType()
            .getAggregateInfo()
            .getIterator(targetNode.evaluate(env));
        Collection<Object> result = new LinkedHashSet<>();
        while (elementsIterator.hasNext()) {
            Object element = elementsIterator.next();
            tempVar.set(null, element, env);
            Object transformed = transformer.evaluate(env);
            if (transformed != null) {
                result.add(transformed);
            }
        }
        return CollectionUtils.toArray(result, componentClass);
    }

    @Override
    public IOpenClass getType() {
        return resultType;
    }
}
