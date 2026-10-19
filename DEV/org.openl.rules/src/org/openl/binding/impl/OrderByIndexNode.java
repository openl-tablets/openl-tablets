package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import lombok.Getter;

import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.vm.IRuntimeEnv;

class OrderByIndexNode<T extends Comparable<T>> extends ABoundNode {

    private final ElementVariables variables;
    private final boolean isDecreasing;
    private final IBoundNode orderBy;
    private final IBoundNode targetNode;
    private final Class<?> componentClass;
    @Getter
    private final IOpenClass type;

    OrderByIndexNode(ISyntaxNode syntaxNode,
                     IBoundNode targetNode,
                     IBoundNode orderBy,
                     ElementVariables variables,
                     boolean isDecreasing) {
        super(syntaxNode, variables.children(targetNode, orderBy));
        this.variables = variables;
        this.isDecreasing = isDecreasing;
        this.orderBy = orderBy;
        this.targetNode = targetNode;

        if (targetNode.getType().isArray()) {
            this.componentClass = targetNode.getType().getComponentClass().getInstanceClass();
            this.type = targetNode.getType();
        } else {
            // Collection
            var componentType = variables.element().getType();
            this.componentClass = componentType.getInstanceClass();
            this.type = componentType.getAggregateInfo().getIndexedAggregateType(componentType);
        }
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        var target = targetNode.evaluate(env);
        if (target == null) {
            return null;
        }

        var map = new TreeMap<T, Object>(
                Comparator.<T>nullsLast(isDecreasing ? Comparator.reverseOrder() : Comparator.naturalOrder()));

        variables.forEach(targetNode.getType().getAggregateInfo().getIterator(target),
                env,
                (element, value) -> putElement(map, (T) orderBy.evaluate(env), value));

        var objects = new ArrayList<Object>();
        for (Object element : map.values()) {
            if (element instanceof OrderList list) {
                objects.addAll(list);
            } else {
                objects.add(element);
            }
        }
        return CollectionUtils.toArray(objects, componentClass);
    }

    /**
     * Puts the element by its key. Elements with equal keys are collected into a list in the order they are put.
     */
    private static <K> void putElement(Map<K, Object> map, K key, Object element) {
        var prev = map.put(key, element);
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

    private static class OrderList extends ArrayList<Object> {
        private static final long serialVersionUID = 1L;
    }
}
