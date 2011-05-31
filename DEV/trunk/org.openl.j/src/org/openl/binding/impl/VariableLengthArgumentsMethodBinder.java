package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.List;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;

/**
 * Binder for methods that contains number of arguments in the signature of the same type.
 * So for the method call 'returnType foo(type1 param1, type2 param2, type2 param3)' this binder 
 * will try to find the method 'returnType foo(type1 param1, type2[] params)'.
 * Supports single argument in the signature. 
 * 
 * @author DLiauchuk
 *
 */
public class VariableLengthArgumentsMethodBinder extends ANodeBinder {
    
    private String methodName;
    private IOpenClass[] argumentsTypes;
    private IBoundNode[] children;
        
    public VariableLengthArgumentsMethodBinder(String methodName, IOpenClass[] argumentsTypes, IBoundNode[] children) {
        this.methodName = methodName;    
        if (argumentsTypes == null || argumentsTypes.length < 1) {
            String message = String.format("At least one argument should exist in method signature(%s) " +
            		"to bind it as variable arguments method", methodName);
            throw new OpenlNotCheckedException(message);
        }
        this.argumentsTypes = argumentsTypes.clone();
        if (children == null) {
            String message = String.format("Chldren nodes for method %s cannot be null", methodName);
            throw new OpenlNotCheckedException(message);
        }
        this.children = children.clone();
    }

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        return getVarArgsMethodNode(node, bindingContext, getModifiedMethodArguments(), getIndexOfFirstVarArg());        
    }
    
    /**
     * The method is 'returnType foo(type param1, type param2)', but as there is no such method, parameters 
     * should be modified(wrapped) to array for further searching for method 'returnType foo(type[] params)'.
     * 
     * @return modified income arguments.
     */
    private IOpenClass[] getModifiedMethodArguments() {
        // list of modified method arguments types
        //
        List<IOpenClass> argumentsTypesSequence = new ArrayList<IOpenClass>();
        
        // the index of the first argument(from left to right) that is the member of var args expression.
        //
        int indexOfFirstVarArg = getIndexOfFirstVarArg();
        if (indexOfFirstVarArg > 0) {
            // if there are arguments that cannot be put to the array of same type, just simply reuse them
            //
            for (int i = 0; i < indexOfFirstVarArg; i++) {
                argumentsTypesSequence.add(argumentsTypes[i]);
            }
        }
        
        // get the class of the var args expression
        //
        IOpenClass varArgClass = argumentsTypes[indexOfFirstVarArg];
        
        // add to the arguments type sequence the array type of the varArgClass.
        //
        argumentsTypesSequence.add(varArgClass.getAggregateInfo().getIndexedAggregateType(varArgClass, 1));
        
        return (IOpenClass[]) argumentsTypesSequence.toArray(new IOpenClass[argumentsTypesSequence.size()]);
    }
    
    /** 
     * @return last index in the sequence of method arguments, that is equal to the last argument.<br>
     * E.g. if there are 3 argument types in sequence: <code>[StringOpenClass, IntOpenClass, IntOpenClass]</code>. The
     * result value will be 1.
     */
    // protected for tests
    protected int getIndexOfFirstVarArg() {
        int numberOfAllArguments = argumentsTypes.length;
        // initialize the index of the first var arg value by the last index of
        // sequence
        //
        int firstVarArgIndex = numberOfAllArguments - 1;

        if (argumentsTypes.length >= 1) {
            for (int j = numberOfAllArguments - 2; j >= 0; j--) {
                if (!argumentsTypes[numberOfAllArguments - 1].equals(argumentsTypes[j])) {
                    // as the not equal arguments were found break
                    //
                    break;
                }
                // found previous argument of the same type as the last one
                //
                firstVarArgIndex = j;
            }
        }
        return firstVarArgIndex;
    }
   
    private IBoundNode getVarArgsMethodNode(ISyntaxNode node, IBindingContext bindingContext, 
            IOpenClass[] methodArguments, int indexOfLastEqualArgumentType) {
        IMethodCaller methodWithLastArrayArgument = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, 
            methodName, methodArguments);
        
        // if can`t find the method, return null.
        //
        if (methodWithLastArrayArgument == null) {
            return null;
        }
        // get the component class of var arg expression
        //
        Class<?> componentVarArgClass = 
            methodArguments[methodArguments.length - 1].getInstanceClass().getComponentType();
        
        
        return new VariableArgumentsMethodBoundNode(node, children, methodWithLastArrayArgument, 
            indexOfLastEqualArgumentType, componentVarArgClass);
    }

}
