package org.openl.binding.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.method.NoVarArgOpenClass;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// try to bind given method if its single parameter is an array, considering that there is a method with
// equal name but for component type of array (e.g. the given method is 'double[] calculate(Premium[] premium)'
// try to bind it as 'double calculate(Premium premium)' and call several times on runtime).
//
public class ArrayArgumentsMethodBinder extends ANodeBinder {

    private final Logger log = LoggerFactory.getLogger(ArrayArgumentsMethodBinder.class);

    private String methodName;
    private IOpenClass[] argumentsTypes;
    private IBoundNode[] children;

    public ArrayArgumentsMethodBinder(String methodName, IOpenClass[] argumentsTypes, IBoundNode[] children) {
        this.methodName = methodName;
        this.argumentsTypes = argumentsTypes.clone();
        this.children = children.clone();
    }

    private IBoundNode getMultiCallMethodNode(ISyntaxNode node,
            IBindingContext bindingContext,
            IOpenClass[] methodArguments,
            Deque<Integer> arrayArgArguments) {
        // find method with given name and component type parameter.
        //
        IMethodCaller singleParameterMethodCaller = bindingContext
            .findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, methodName, methodArguments);

        // if can`t find, return null.
        //
        if (singleParameterMethodCaller == null) {
            return null;
        }

        BindHelper.checkOnDeprecation(node, bindingContext, singleParameterMethodCaller);
        // bound node that is going to call the single parameter method by several times on runtime and return an array
        // of results.
        //
        return makeMultiCallMethodBoundNode(node,
            children,
            new ArrayList<>(arrayArgArguments),
            singleParameterMethodCaller);
    }

    protected IBoundNode makeMultiCallMethodBoundNode(ISyntaxNode node,
            IBoundNode[] children,
            List<Integer> arrayArgArgumentList,
            IMethodCaller singleParameterMethodCaller) {
        return new MultiCallMethodBoundNode(node, children, singleParameterMethodCaller, arrayArgArgumentList);
    }

    private IBoundNode getMultiCallMethodNode(ISyntaxNode node,
            IBindingContext bindingContext,
            IOpenClass[] unwrappedArgumentsTypes,
            Deque<Integer> arrayArgArguments,
            List<Integer> indexesOfArrayArguments,
            int indexToChange) {
        int arrayArgumentIndex = indexesOfArrayArguments.get(indexToChange);

        boolean last = indexToChange == indexesOfArrayArguments.size() - 1;

        IBoundNode multiCallMethodNode;

        // Try interpret array argument as is, not multicall
        unwrappedArgumentsTypes[arrayArgumentIndex] = argumentsTypes[arrayArgumentIndex];
        multiCallMethodNode = last ? getMultiCallMethodNode(node,
            bindingContext,
            unwrappedArgumentsTypes,
            arrayArgArguments)
                                   : getMultiCallMethodNode(node,
                                       bindingContext,
                                       unwrappedArgumentsTypes,
                                       arrayArgArguments,
                                       indexesOfArrayArguments,
                                       indexToChange + 1);

        if (multiCallMethodNode != null) {
            return multiCallMethodNode;
        }

        // Try interpret array argument as multicall
        arrayArgArguments.addLast(arrayArgumentIndex);
        unwrappedArgumentsTypes[arrayArgumentIndex] = new NoVarArgOpenClass(argumentsTypes[arrayArgumentIndex].getComponentClass());
        multiCallMethodNode = last ? getMultiCallMethodNode(node,
            bindingContext,
            unwrappedArgumentsTypes,
            arrayArgArguments)
                                   : getMultiCallMethodNode(node,
                                       bindingContext,
                                       unwrappedArgumentsTypes,
                                       arrayArgArguments,
                                       indexesOfArrayArguments,
                                       indexToChange + 1);
        arrayArgArguments.removeLast();

        if (multiCallMethodNode != null) {
            return multiCallMethodNode;
        }

        return null;
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
            return getMultiCallMethodNode(node,
                bindingContext,
                unwrappedArgumentsTypes,
                arrayArgArguments,
                indexesOfArrayArguments,
                0);
        } else {
            log.debug("There is no any array argument in signature for method '{}'", methodName);
        }
        return null;
    }
}
