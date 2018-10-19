package org.openl.binding.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaArrayAggregateInfo;
import org.openl.vm.IRuntimeEnv;

class TransformIndexNode extends ABoundNode {
    private ILocalVar tempVar;
    private final boolean isUnique;
    private IBoundNode transformer;
    private IBoundNode targetNode;

    TransformIndexNode(ISyntaxNode syntaxNode,
            IBoundNode targetNode,
            IBoundNode transformer,
            ILocalVar tempVar,
            boolean isUnique) {
        super(syntaxNode, targetNode, transformer);
        this.tempVar = tempVar;
        this.isUnique = isUnique;
        this.targetNode = targetNode;
        this.transformer = transformer;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Iterator<Object> elementsIterator = targetNode.getType().getAggregateInfo().getIterator(
            targetNode.evaluate(env));
        List<Object> firedElements = new ArrayList<>();
        HashSet<Object> uniqueSet = null;
        if (isUnique) {
            uniqueSet = new HashSet<>();
        }
        while (elementsIterator.hasNext()) {
            Object element = elementsIterator.next();
            tempVar.set(null, element, env);
            Object transformed = transformer.evaluate(env);
            if (isUnique) {
                if (uniqueSet.add(transformed)) {
                    firedElements.add(transformed);
                }
            } else {
                firedElements.add(transformed);
            }

        }
        Object result = Array.newInstance(transformer.getType().getInstanceClass(), firedElements.size());
        for (int i = 0; i < firedElements.size(); i++) {
            Array.set(result, i, firedElements.get(i));
        }
        return result;
    }

    public IOpenClass getType() {
        IOpenClass targetType = transformer.getType();
        return JavaArrayAggregateInfo.ARRAY_AGGREGATE.getIndexedAggregateType(targetType, 1);
    }
}
