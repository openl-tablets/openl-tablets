package org.openl.binding.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

class SplitByIndexNode extends ABoundNode {

    private final ILocalVar tempVar;
    private final IBoundNode splitBy;
    private final IBoundNode targetNode;
    private final IOpenCast openCast;
    private final IOpenClass type;
    private final IOpenClass componentType;

    SplitByIndexNode(ISyntaxNode syntaxNode,
            IBoundNode targetNode,
            IBoundNode splitBy,
            ILocalVar tempVar,
            IOpenCast openCast) {
        super(syntaxNode, targetNode, splitBy);
        this.tempVar = tempVar;
        this.targetNode = targetNode;
        this.splitBy = splitBy;
        this.openCast = openCast;

        if (targetNode.getType().isArray()) {
            this.componentType = targetNode.getType().getComponentClass();
            this.type = targetNode.getType().getAggregateInfo().getIndexedAggregateType(targetNode.getType());
        } else {
            this.componentType = tempVar.getType();
            // the first dimension
            IOpenClass ct = componentType.getAggregateInfo().getIndexedAggregateType(componentType);
            // the second dimension
            this.type = ct.getAggregateInfo().getIndexedAggregateType(ct);
        }
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        Object target = targetNode.evaluate(env);
        if (target == null) {
            return null;
        }

        IOpenClass containerType = targetNode.getType();
        IAggregateInfo aggregateInfo = containerType.getAggregateInfo();
        Iterator<Object> elementsIterator = aggregateInfo.getIterator(target);

        Object tempKey = new Object();

        HashMap<Object, ArrayList<Object>> map = new HashMap<>();
        ArrayList<ArrayList<Object>> list2d = new ArrayList<>();

        while (elementsIterator.hasNext()) {
            Object element = elementsIterator.next();
            if (element == null) {
                continue;
            }
            Object converted = openCast != null ? openCast.convert(element) : element;
            tempVar.set(null, converted, env);
            Object key = splitBy.evaluate(env);

            if (key == null) {
                key = tempKey;
            }

            ArrayList<Object> list = map.get(key);

            if (list == null) {
                list = new ArrayList<>();
                map.put(key, list);
                list2d.add(list);
            }

            list.add(element);
        }

        int size = list2d.size();

        IOpenClass arrayType = componentType.getAggregateInfo().getIndexedAggregateType(componentType);
        Object result = Array.newInstance(arrayType.getInstanceClass(), size);

        for (int i = 0; i < size; i++) {

            ArrayList<Object> list = list2d.get(i);
            int listSize = list.size();

            Object ary = Array.newInstance(componentType.getInstanceClass(), listSize);

            for (int j = 0; j < listSize; j++) {
                Array.set(ary, j, list.get(j));
            }

            Array.set(result, i, ary);

        }

        return result;
    }

    @Override
    public IOpenClass getType() {
        return type;
    }
}
