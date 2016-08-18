package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.List;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
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

    private IBoundNode getMultiCallMethodNode(ISyntaxNode node, IBindingContext bindingContext,
                                              IOpenClass[] methodArguments, List<Integer> arrayArgArguments) {
        // find method with given name and component type parameter.
        //
        IMethodCaller singleParameterMethodCaller = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, methodName, methodArguments);

        // if can`t find, return null.
        //
        if (singleParameterMethodCaller == null) {
            return null;
        }

        // bound node that is going to call the single parameter method by several times on runtime and return an array 
        // of results.
        //
        return new MultiCallMethodBoundNode(node, children, singleParameterMethodCaller, arrayArgArguments);
    }

    private List<Integer> getIndexesOfArrayArguments() {
        List<Integer> indexes = new ArrayList<Integer>();
        for (int i = 0; i < argumentsTypes.length; i++) {
            if (argumentsTypes[i].isArray()) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        List<Integer> indexesOfArrayArguments = getIndexesOfArrayArguments();
        if (indexesOfArrayArguments.size() > 0) {
            IOpenClass[] unwrappedArgumentsTypes = new IOpenClass[argumentsTypes.length];
            System.arraycopy(argumentsTypes, 0, unwrappedArgumentsTypes, 0, argumentsTypes.length);

            List<Integer> arrayArgArguments = new ArrayList<Integer>();
            for (Integer arrayArgumentIndex : indexesOfArrayArguments) {
                unwrappedArgumentsTypes[arrayArgumentIndex] = argumentsTypes[arrayArgumentIndex].getComponentClass();

                arrayArgArguments.add(arrayArgumentIndex);

                IBoundNode multiCallMethodNode = getMultiCallMethodNode(node,
                        bindingContext,
                        unwrappedArgumentsTypes,
                        arrayArgArguments);
                if (multiCallMethodNode != null) {
                    return multiCallMethodNode;
                }
            }
        } else {
            log.debug("There is no any array argument in signature for {} method", methodName);
        }
        return null;
    }
}
