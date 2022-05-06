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
import org.openl.binding.impl.method.MethodSearch;
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

    private static final Logger LOG = LoggerFactory.getLogger(ArrayArgumentsMethodBinder.class);
    private static final int MAX_COUNT_OF_ARGUMENTS_TO_MULTI_CALL = 5;

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
            MutableObject<MultiCallMethodBoundNode> best,
            IOpenClass targetType) {
        if (countOfChanged > bestCountOfChanged.getValue() || arrayArgArguments.isEmpty()) {
            return;
        }
        // find method with given name and component type parameter.
        //
        IMethodCaller singleParameterMethodCaller;
        try {
            if (targetType == null) {
                singleParameterMethodCaller = bindingContext
                    .findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, methodName, methodArguments);
            } else {
                singleParameterMethodCaller = MethodSearch
                    .findMethod(methodName, methodArguments, bindingContext, targetType);
            }
        } catch (AmbiguousMethodException e) {
            if (countOfChanged < bestCountOfChanged.getValue()) {
                candidates.clear();
            }
            for (IOpenMethod openMethod : e.getMatchingMethods()) {
                candidates.put(new MethodKey(openMethod), openMethod);
            }
            LOG.debug("Error occurred: ", e);
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

            // bound node that is going to call the single parameter method by several times on runtime and return an
            // array
            // of results.
            //
            MultiCallMethodBoundNode multiCallMethodBoundNode = makeMultiCallMethodBoundNode(node,
                children,
                new ArrayList<>(arrayArgArguments),
                singleParameterMethodCaller);
            best.setValue(multiCallMethodBoundNode);
        }
        candidates.put(new MethodKey(singleParameterMethodCaller.getMethod()), singleParameterMethodCaller.getMethod());
    }

    protected MultiCallMethodBoundNode makeMultiCallMethodBoundNode(ISyntaxNode node,
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
            MutableObject<MultiCallMethodBoundNode> best,
            IOpenClass targetType) {
        int arrayArgumentIndex = indexesOfArrayArguments.get(indexToChange);

        boolean last = indexToChange == indexesOfArrayArguments.size() - 1;

        // Try to interpret array argument as is, not multicall
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
            last,
            targetType);
        // Try to interpret array argument as multicall
        if (countOfChanged < MAX_COUNT_OF_ARGUMENTS_TO_MULTI_CALL) {
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
                last,
                targetType);
            arrayArgArguments.removeLast();
        }
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
            MutableObject<MultiCallMethodBoundNode> best,
            boolean last,
            IOpenClass targetType) {
        if (last) {
            matchMultiCallMethodNode(node,
                bindingContext,
                unwrappedArgumentsTypes,
                arrayArgArguments,
                countOfChanged,
                bestCountOfChanged,
                candidates,
                best,
                targetType);
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
                best,
                targetType);
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
        return bindInternal(node, bindingContext, null);
    }

    private IBoundNode bindInternal(ISyntaxNode node, IBindingContext bindingContext, IOpenClass targetType) {
        List<Integer> indexesOfArrayArguments = getIndexesOfArrayArguments();
        if (!indexesOfArrayArguments.isEmpty()) {
            IOpenClass[] unwrappedArgumentsTypes = new IOpenClass[argumentsTypes.length];
            System.arraycopy(argumentsTypes, 0, unwrappedArgumentsTypes, 0, argumentsTypes.length);

            ArrayDeque<Integer> arrayArgArguments = new ArrayDeque<>();
            Map<MethodKey, IOpenMethod> candidates = new HashMap<>();
            MutableInt bestCountOfChanges = new MutableInt(Integer.MAX_VALUE);
            MutableObject<MultiCallMethodBoundNode> best = new MutableObject<>(null);
            recursiveMultiCallMethodNodeSearch(node,
                bindingContext,
                unwrappedArgumentsTypes,
                arrayArgArguments,
                indexesOfArrayArguments,
                0,
                0,
                bestCountOfChanges,
                candidates,
                best,
                targetType);
            if (candidates.size() > 1) {
                throw new AmbiguousMethodException(methodName, argumentsTypes, new ArrayList<>(candidates.values()));
            }
            MultiCallMethodBoundNode multiCallMethodBoundNode = best.getValue();
            if (multiCallMethodBoundNode != null) {
                BindHelper.checkOnDeprecation(node, bindingContext, multiCallMethodBoundNode.getMethodCaller());
            }
            return multiCallMethodBoundNode;
        } else {
            LOG.debug("There is no any array argument in signature for method '{}'", methodName);
        }
        return null;
    }

    @Override
    public IBoundNode bindTarget(ISyntaxNode node,
            IBindingContext bindingContext,
            IBoundNode targetNode) throws Exception {
        IOpenClass type = targetNode.getType();
        return bindInternal(node, bindingContext, type);
    }
}
