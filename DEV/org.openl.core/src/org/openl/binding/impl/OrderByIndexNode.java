package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;

import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.vm.IRuntimeEnv;

class OrderByIndexNode<T extends Comparable<T>> extends ABoundNode {

    private final ILocalVar tempVar;
    private final boolean isDecreasing;
    private final IBoundNode orderBy;
    private final IBoundNode targetNode;
    private final IOpenCast openCast;
    private final Class<?> componentClass;
    private final IOpenClass type;

    OrderByIndexNode(ISyntaxNode syntaxNode,
            IBoundNode targetNode,
            IBoundNode orderBy,
            ILocalVar tempVar,
            IOpenCast openCast,
            boolean isDecreasing) {
        super(syntaxNode, targetNode, orderBy);
        this.tempVar = tempVar;
        this.isDecreasing = isDecreasing;
        this.orderBy = orderBy;
        this.targetNode = targetNode;
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

        TreeMap<T, Object> map = new TreeMap<>(
            Comparator.<T> nullsLast(isDecreasing ? Comparator.reverseOrder() : Comparator.naturalOrder()));

        while (elementsIterator.hasNext()) {
            Object element = elementsIterator.next();
            if (element == null) {
                continue;
            }
            element = openCast != null ? openCast.convert(element) : element;
            tempVar.set(null, element, env);
            T key = (T) orderBy.evaluate(env);
            Object prev = map.put(key, element);
            if (prev != null) {
                OrderList list;
                if (prev.getClass() != OrderList.class) {
                    list = new OrderList();
                    list.add(prev);
                } else {
                    list = (OrderList) prev;
                }

                list.add(element);
                map.put(key, list);
            }
        }

        ArrayList<Object> objects = new ArrayList<>();
        for (Object element : map.values()) {
            if (element instanceof OrderList) {
                objects.addAll((OrderList) element);
            } else {
                objects.add(element);
            }
        }
        return CollectionUtils.toArray(objects, componentClass);
    }

    @Override
    public IOpenClass getType() {
        return type;
    }

    private static class OrderList extends ArrayList<Object> {
        private static final long serialVersionUID = 1L;
    }
}
