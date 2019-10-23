package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;

import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaArrayAggregateInfo;
import org.openl.util.CollectionUtils;
import org.openl.vm.IRuntimeEnv;

class OrderByIndexNode extends ABoundNode {

    private static final Comparator<Comparable<Object>> ASC = new AscComparator<>();
    private static final Comparator<Comparable<Object>> DESC = new DescComparator<>();
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
        Object target = targetNode.evaluate(env);
        if (target == null) {
            return null;
        }

        IAggregateInfo aggregateInfo = targetNode.getType().getAggregateInfo();
        Iterator<Object> elementsIterator = aggregateInfo.getIterator(target);

        TreeMap<Comparable<Object>, Object> map = new TreeMap<>(isDecreasing ? DESC : ASC);

        while (elementsIterator.hasNext()) {
            Object element = elementsIterator.next();
            if (element == null) {
                continue;
            }
            tempVar.set(null, element, env);
            Comparable<Object> key = (Comparable<Object>) orderBy.evaluate(env);
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
                for (Object item : (OrderList) element) {
                    objects.add(item);
                }
            } else {
                objects.add(element);
            }
        }
        Class<?> instanceClass = tempVar.getType().getInstanceClass();
        return CollectionUtils.toArray(objects, instanceClass);
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

    private static class OrderList extends ArrayList<Object> {
        private static final long serialVersionUID = 1L;
    }

    private static class AscComparator<T extends Comparable<Object>> implements Comparator<T> {

        @Override
        public int compare(T o1, T o2) {
            if (o1 == o2) {
                return 0;
            } else if (o1 == null) {// Move nulls to the end
                return 1;
            } else if (o2 == null) {
                return -1;
            }
            return o1.compareTo(o2);
        }
    }

    private static class DescComparator<T extends Comparable<Object>> implements Comparator<T> {

        @Override
        public int compare(T o1, T o2) {
            if (o1 == o2) {
                return 0;
            } else if (o1 == null) {// Move nulls to the end
                return 1;
            } else if (o2 == null) {
                return -1;
            }
            return o2.compareTo(o1);
        }
    }
}
