package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.util.BooleanUtils;
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
        List<Object> firedElements = new ArrayList<Object>();
        while (elementsIterator.hasNext()) {
            Object element = elementsIterator.next();
            tempVar.set(null, element, env);
            if (BooleanUtils.toBoolean(condition.evaluate(env))) {
                firedElements.add(element);
            }
        }
        Object result = aggregateInfo.makeIndexedAggregate(tempVar.getType(), new int[] { firedElements.size() });
        IOpenIndex index = aggregateInfo.getIndex(targetNode.getType());
        for (int i = 0; i < firedElements.size(); i++) {
            index.setValue(result, i, firedElements.get(i));
        }
        return result;
    }

    public IOpenClass getType() {
        IOpenClass type = targetNode.getType();
        if (type.isArray()) {
            return type;
        }

        if (type.getAggregateInfo() != null && type.getAggregateInfo().isAggregate(type)) {
            return type;
        }

        IOpenClass varType = tempVar.getType();
        return varType.getAggregateInfo().getIndexedAggregateType(varType, 1);
    }
}
