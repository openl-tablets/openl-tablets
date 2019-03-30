package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaArrayAggregateInfo;
import org.openl.util.BooleanUtils;
import org.openl.util.CollectionUtils;
import org.openl.vm.IRuntimeEnv;

class SelectAllIndexNode extends ABoundNode {
    private ILocalVar tempVar;
    private IBoundNode condition;
    private IBoundNode targetNode;

    SelectAllIndexNode(ISyntaxNode syntaxNode, IBoundNode targetNode, IBoundNode condition, ILocalVar tempVar) {
        super(syntaxNode, targetNode, condition);
        this.tempVar = tempVar;
        this.targetNode = targetNode;
        this.condition = condition;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        IAggregateInfo aggregateInfo = targetNode.getType().getAggregateInfo();
        Iterator<Object> elementsIterator = aggregateInfo.getIterator(targetNode.evaluate(env));
        List<Object> firedElements = new ArrayList<>();
        while (elementsIterator.hasNext()) {
            Object element = elementsIterator.next();
            if (element == null) {
                continue;
            }
            tempVar.set(null, element, env);
            if (BooleanUtils.toBoolean(condition.evaluate(env))) {
                firedElements.add(element);
            }
        }
        Class<?> instanceClass = tempVar.getType().getInstanceClass();
        return CollectionUtils.toArray(firedElements, instanceClass);
    }

    @Override
    public IOpenClass getType() {
        IOpenClass type = targetNode.getType();
        if (type.isArray()) {
            return type;
        }

        IOpenClass varType = tempVar.getType();
        return JavaArrayAggregateInfo.ARRAY_AGGREGATE.getIndexedAggregateType(varType);
    }
}
