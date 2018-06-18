package org.openl.binding.impl;

import java.util.Iterator;

import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.util.BooleanUtils;
import org.openl.vm.IRuntimeEnv;

class SelectFirstIndexNode extends ABoundNode {
    private ILocalVar tempVar;
    private IBoundNode condition;
    private IBoundNode targetNode;

    SelectFirstIndexNode(ISyntaxNode syntaxNode, IBoundNode targetNode, IBoundNode condition, ILocalVar tempVar) {
        super(syntaxNode, targetNode, condition);
        this.tempVar = tempVar;
        this.targetNode = targetNode;
        this.condition = condition;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        IAggregateInfo aggregateInfo = targetNode.getType().getAggregateInfo();
        Iterator<Object> elementsIterator = aggregateInfo.getIterator(targetNode.evaluate(env));
        while (elementsIterator.hasNext()) {
            Object element = elementsIterator.next();
            tempVar.set(null, element, env);
            if (BooleanUtils.toBoolean(condition.evaluate(env))) {
                return element;
            }
        }
        return null;
    }

    public IOpenClass getType() {
        IOpenClass type = targetNode.getType();
        return type.getAggregateInfo().getComponentType(type);
    }
}
