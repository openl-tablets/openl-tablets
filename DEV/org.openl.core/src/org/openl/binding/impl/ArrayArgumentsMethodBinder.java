package org.openl.binding.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.impl.cast.IOneElementArrayCast;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.CastingMethodCaller;
import org.openl.types.impl.MethodKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// try to bind given method if its single parameter is an array, considering that there is a method with
// equal name but for component type of array (e.g. the given method is 'double[] calculate(Premium[] premium)'
// try to bind it as 'double calculate(Premium premium)' and call several times on runtime).
//
public class ArrayArgumentsMethodBinder extends ANodeBinder {

    private final Logger log = LoggerFactory.getLogger(ArrayArgumentsMethodBinder.class);

    private final String methodName;
    private final IOpenClass[] argumentsTypes;
    private final IBoundNode[] children;

    public ArrayArgumentsMethodBinder(String methodName, IOpenClass[] argumentsTypes, IBoundNode[] children) {
        this.methodName = methodName;
        this.argumentsTypes = argumentsTypes.clone();
        this.children = children.clone();
    }

    private void matchMultiCallMethodNode(ISyntaxNode node,
            IBindingContext bindingContext,
            IOpenClass[] methodArguments,
            Deque<Integer> arrayArgArguments,
            int countOfChanged,
            MutableInt bestCountOfChanged,
            Map<MethodKey, IOpenMethod> candidates,
            MutableObject<IBoundNode> best) {
        if (countOfChanged > bestCountOfChanged.getValue() || arrayArgArguments.isEmpty()) {
            return;
        }
        // find method with given name and component type parameter.
        //
        IMethodCaller singleParameterMethodCaller;
        try {
            singleParameterMethodCaller = bindingContext
                .findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, methodName, methodArguments);
        } catch (AmbiguousMethodException e) {
            if (countOfChanged < bestCountOfChanged.getValue()) {
                candidates.clear();
            }
            for (IOpenMethod openMethod : e.getMatchingMethods()) {
                candidates.put(new MethodKey(openMethod), openMethod);
            }
            return;
        }

        if (singleParameterMethodCaller instanceof CastingMethodCaller) {
            CastingMethodCaller castingMethodCaller = (CastingMethodCaller) singleParameterMethodCaller;
            int i = 0;
            for (IOpenCast openCast : castingMethodCaller.getCasts()) {
                if (openCast instanceof IOneElementArrayCast && arrayArgArguments.contains(i)) {
                    return;
                }
                i++;
            }
        }

        // if can`t find, return null.
        //
        if (singleParameterMethodCaller == null) {
            return;
        }

        if (countOfChanged < bestCountOfChanged.getValue()) {
            candidates.clear();
            bestCountOfChanged.setValue(countOfChanged);

            BindHelper.checkOnDeprecation(node, bindingContext, singleParameterMethodCaller);
            // bound node that is going to call the single parameter method by several times on runtime and return an
            // array
            // of results.
            //
            IBoundNode multiCallMethodBoundNode = makeMultiCallMethodBoundNode(node,
                children,
                new ArrayList<>(arrayArgArguments),
                singleParameterMethodCaller);
            best.setValue(multiCallMethodBoundNode);
        }
        candidates.put(new MethodKey(singleParameterMethodCaller.getMethod()), singleParameterMethodCaller.getMethod());
    }

    protected IBoundNode makeMultiCallMethodBoundNode(ISyntaxNode node,
            IBoundNode[] children,
            List<Integer> arrayArgArgumentList,
            IMethodCaller singleParameterMethodCaller) {
        return new MultiCallMethodBoundNode(node, children, singleParameterMethodCaller, arrayArgArgumentList);
    }

    private void recursiveMultiCallMethodNodeSearch(ISyntaxNode node,
            IBindingContext bindingContext,
            IOpenClass[] unwrappedArgumentsTypes,
            Deque<Integer> arrayArgArguments,
            List<Integer> indexesOfArrayArguments,
            int indexToChange,
            int countOfChanged,
            MutableInt bestCountOfChanged,
            Map<MethodKey, IOpenMethod> candidates,
            MutableObject<IBoundNode> best) {
        int arrayArgumentIndex = indexesOfArrayArguments.get(indexToChange);

        boolean last = indexToChange == indexesOfArrayArguments.size() - 1;

        // Try interpret array argument as is, not multicall
        unwrappedArgumentsTypes[arrayArgumentIndex] = argumentsTypes[arrayArgumentIndex];
        splitByLastToFindMultiCallMethodNode(node,
            bindingContext,
            unwrappedArgumentsTypes,
            arrayArgArguments,
            indexesOfArrayArguments,
            indexToChange,
            countOfChanged,
            bestCountOfChanged,
            candidates,
            best,
            last);
        // Try interpret array argument as multicall
        arrayArgArguments.addLast(arrayArgumentIndex);
        unwrappedArgumentsTypes[arrayArgumentIndex] = argumentsTypes[arrayArgumentIndex].getComponentClass();
        splitByLastToFindMultiCallMethodNode(node,
            bindingContext,
            unwrappedArgumentsTypes,
            arrayArgArguments,
            indexesOfArrayArguments,
            indexToChange,
            countOfChanged + 1,
            bestCountOfChanged,
            candidates,
            best,
            last);
        arrayArgArguments.removeLast();
    }

    private void splitByLastToFindMultiCallMethodNode(ISyntaxNode node,
            IBindingContext bindingContext,
            IOpenClass[] unwrappedArgumentsTypes,
            Deque<Integer> arrayArgArguments,
            List<Integer> indexesOfArrayArguments,
            int indexToChange,
            int countOfChanged,
            MutableInt bestCountOfChanged,
            Map<MethodKey, IOpenMethod> candidates,
            MutableObject<IBoundNode> best,
            boolean last) {
        if (last) {
            matchMultiCallMethodNode(node,
                bindingContext,
                unwrappedArgumentsTypes,
                arrayArgArguments,
                countOfChanged,
                bestCountOfChanged,
                candidates,
                best);
        } else {
            recursiveMultiCallMethodNodeSearch(node,
                bindingContext,
                unwrappedArgumentsTypes,
                arrayArgArguments,
                indexesOfArrayArguments,
                indexToChange + 1,
                countOfChanged,
                bestCountOfChanged,
                candidates,
                best);
        }
    }

    private List<Integer> getIndexesOfArrayArguments() {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < argumentsTypes.length; i++) {
            if (argumentsTypes[i].isArray()) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        List<Integer> indexesOfArrayArguments = getIndexesOfArrayArguments();
        if (!indexesOfArrayArguments.isEmpty()) {
            IOpenClass[] unwrappedArgumentsTypes = new IOpenClass[argumentsTypes.length];
            System.arraycopy(argumentsTypes, 0, unwrappedArgumentsTypes, 0, argumentsTypes.length);

            ArrayDeque<Integer> arrayArgArguments = new ArrayDeque<>();
            Map<MethodKey, IOpenMethod> candidates = new HashMap<>();
            MutableInt bestCountOfChanges = new MutableInt(Integer.MAX_VALUE);
            MutableObject<IBoundNode> best = new MutableObject<>(null);
            recursiveMultiCallMethodNodeSearch(node,
                bindingContext,
                unwrappedArgumentsTypes,
                arrayArgArguments,
                indexesOfArrayArguments,
                0,
                0,
                bestCountOfChanges,
                candidates,
                best);
            if (candidates.size() > 1) {
                throw new AmbiguousMethodException(methodName, argumentsTypes, new ArrayList<>(candidates.values()));
            }
            return best.getValue();
        } else {
            log.debug("There is no any array argument in signature for method '{}'", methodName);
        }
        return null;
    }
}
