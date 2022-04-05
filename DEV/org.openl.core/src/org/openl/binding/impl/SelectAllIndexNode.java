package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.util.BooleanUtils;
import org.openl.util.CollectionUtils;
import org.openl.vm.IRuntimeEnv;

class SelectAllIndexNode extends ABoundNode {
    private final ILocalVar tempVar;
    private final IBoundNode condition;
    private final IBoundNode targetNode;
    private final IOpenCast openCast;
    private final Class<?> componentClass;
    private final IOpenClass type;

    SelectAllIndexNode(ISyntaxNode syntaxNode,
            IBoundNode targetNode,
            IBoundNode condition,
            ILocalVar tempVar,
            IOpenCast openCast) {
        super(syntaxNode, targetNode, condition);
        this.tempVar = tempVar;
        this.targetNode = targetNode;
        this.condition = condition;
        this.openCast = openCast;

        if (targetNode.getType().isArray()) {
            this.componentClass = targetNode.getType().getComponentClass().getInstanceClass();
            this.type = targetNode.getType();
        } else {
            // Collection
            this.componentClass = tempVar.getType().getInstanceClass();
            IOpenClass componentType = tempVar.getType();
            this.type = componentType.getAggregateInfo().getIndexedAggregateType(componentType);
        }
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object target = targetNode.evaluate(env);
        if (target == null) {
            return null;
        }
        IAggregateInfo aggregateInfo = targetNode.getType().getAggregateInfo();
        Iterator<Object> elementsIterator = aggregateInfo.getIterator(target);
        List<Object> firedElements = new ArrayList<>();
        while (elementsIterator.hasNext()) {
            Object element = elementsIterator.next();
            if (element == null) {
                continue;
            }
            Object converted = openCast != null ? openCast.convert(element) : element;
            tempVar.set(null, converted, env);
            if (BooleanUtils.toBoolean(condition.evaluate(env))) {
                firedElements.add(element);
            }
        }
        return CollectionUtils.toArray(firedElements, componentClass);
    }

    @Override
    public IOpenClass getType() {
        return type;
    }
}
