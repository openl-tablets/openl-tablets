package org.openl.binding.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
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
            var ct = componentType.getAggregateInfo().getIndexedAggregateType(componentType);
            // the second dimension
            this.type = ct.getAggregateInfo().getIndexedAggregateType(ct);
        }
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {
        var target = targetNode.evaluate(env);
        if (target == null) {
            return null;
        }

        var containerType = targetNode.getType();
        var aggregateInfo = containerType.getAggregateInfo();
        var elementsIterator = aggregateInfo.getIterator(target);

        var tempKey = new Object();

        var map = new HashMap<Object, ArrayList<Object>>();
        var list2d = new ArrayList<ArrayList<Object>>();

        while (elementsIterator.hasNext()) {
            var element = elementsIterator.next();
            if (element == null) {
                continue;
            }
            Object converted = openCast != null ? openCast.convert(element) : element;
            tempVar.set(null, converted, env);
            var key = splitBy.evaluate(env);

            if (key == null) {
                key = tempKey;
            }

            var list = map.get(key);

            if (list == null) {
                list = new ArrayList<>();
                map.put(key, list);
                list2d.add(list);
            }

            list.add(element);
        }

        var size = list2d.size();

        var arrayType = componentType.getAggregateInfo().getIndexedAggregateType(componentType);
        Object result = Array.newInstance(arrayType.getInstanceClass(), size);

        for (var i = 0; i < size; i++) {

            var list = list2d.get(i);
            var listSize = list.size();

            Object ary = Array.newInstance(componentType.getInstanceClass(), listSize);

            for (var j = 0; j < listSize; j++) {
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
