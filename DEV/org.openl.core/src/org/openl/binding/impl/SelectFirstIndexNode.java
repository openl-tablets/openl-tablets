package org.openl.binding.impl;

import java.util.Iterator;

import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.util.BooleanUtils;
import org.openl.vm.IRuntimeEnv;

class SelectFirstIndexNode extends ABoundNode {
    private final ILocalVar tempVar;
    private final IBoundNode condition;
    private final IBoundNode targetNode;
    private final IOpenCast openCast;

    SelectFirstIndexNode(ISyntaxNode syntaxNode,
            IBoundNode targetNode,
            IBoundNode condition,
            ILocalVar tempVar,
            IOpenCast openCast) {
        super(syntaxNode, targetNode, condition);
        this.tempVar = tempVar;
        this.targetNode = targetNode;
        this.condition = condition;
        this.openCast = openCast;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object target = targetNode.evaluate(env);
        if (target == null) {
            return null;
        }
        IAggregateInfo aggregateInfo = targetNode.getType().getAggregateInfo();
        Iterator<Object> elementsIterator = aggregateInfo.getIterator(target);
        while (elementsIterator.hasNext()) {
            Object element = elementsIterator.next();
            if (element == null) {
                continue;
            }
            Object converted = openCast != null ? openCast.convert(element) : element;
            tempVar.set(null, converted, env);
            if (BooleanUtils.toBoolean(condition.evaluate(env))) {
                return element;
            }
        }
        return null;
    }

    @Override
    public IOpenClass getType() {
        IOpenClass type = targetNode.getType();
        return type.getAggregateInfo().getComponentType(type);
    }
}
