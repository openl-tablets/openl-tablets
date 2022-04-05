package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.Iterator;

import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.vm.IRuntimeEnv;

class TransformIndexNode extends ABoundNode {
    private final ILocalVar tempVar;
    private final IBoundNode transformer;
    private final IBoundNode targetNode;
    private final IOpenCast openCast;

    TransformIndexNode(ISyntaxNode syntaxNode,
            IBoundNode targetNode,
            IBoundNode transformer,
            ILocalVar tempVar,
            IOpenCast openCast) {
        super(syntaxNode, targetNode, transformer);
        this.tempVar = tempVar;
        this.targetNode = targetNode;
        this.transformer = transformer;
        this.openCast = openCast;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object target = targetNode.evaluate(env);
        if (target == null) {
            return null;
        }
        Iterator<Object> elementsIterator = targetNode.getType().getAggregateInfo().getIterator(target);
        ArrayList<Object> result = new ArrayList<>();
        while (elementsIterator.hasNext()) {
            Object element = elementsIterator.next();
            element = openCast != null ? openCast.convert(element) : element;
            tempVar.set(null, element, env);
            Object transformed = transformer.evaluate(env);
            result.add(transformed);
        }
        return CollectionUtils.toArray(result, transformer.getType().getInstanceClass());
    }

    @Override
    public IOpenClass getType() {
        IOpenClass componentType = transformer.getType();
        return componentType.getAggregateInfo().getIndexedAggregateType(componentType);
    }
}
