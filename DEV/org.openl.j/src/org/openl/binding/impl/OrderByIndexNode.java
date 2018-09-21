package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeMap;

import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.vm.IRuntimeEnv;

class OrderByIndexNode extends ABoundNode {

    private ILocalVar tempVar;
    private boolean isDecreasing;
    private IBoundNode orderBy;
    private IBoundNode targetNode;

    OrderByIndexNode(ISyntaxNode syntaxNode,
            IBoundNode targetNode,
            IBoundNode orderBy,
            ILocalVar tempVar,
            boolean isDecreasing) {
        super(syntaxNode, targetNode, orderBy);
        this.tempVar = tempVar;
        this.isDecreasing = isDecreasing;
        this.orderBy = orderBy;
        this.targetNode = targetNode;
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        IAggregateInfo aggregateInfo = targetNode.getType().getAggregateInfo();
        Object container = targetNode.evaluate(env);

        Iterator<Object> elementsIterator = aggregateInfo.getIterator(container);

        TreeMap<Comparable<?>, Object> map = initTreeMap();

        int size = 0;
        while (elementsIterator.hasNext()) {
            Object element = elementsIterator.next();
            tempVar.set(null, element, env);
            Comparable<?> key = (Comparable<?>) orderBy.evaluate(env);
            Object prev = map.put(key, element);
            if (prev != null) {
                OrderList list;
                if (prev.getClass() != OrderList.class) {
                    list = new OrderList();
                    list.add(prev);
                } else
                    list = (OrderList) prev;

                list.add(element);
                map.put(key, list);
            }
            ++size;
        }

        Object result = aggregateInfo.makeIndexedAggregate(aggregateInfo.getComponentType(getType()), size);

        IOpenIndex index = aggregateInfo.getIndex(targetNode.getType());
        int idx = 0;
        for (Object element : map.values()) {
            if (element.getClass() != OrderList.class) {
                index.setValue(result, idx++, element);
            } else {
                for (Object item : (OrderList) element) {
                    index.setValue(result, idx++, item);
                }
            }
        }
        return result;
    }

    private TreeMap<Comparable<?>, Object> initTreeMap() {
        if (isDecreasing) {
            return new TreeMap<>(Collections.reverseOrder());
        }
        return new TreeMap<>();
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

    private static class OrderList extends ArrayList<Object> {
        private static final long serialVersionUID = 1L;
    }
}
