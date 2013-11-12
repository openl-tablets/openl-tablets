package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.List;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.IOpenCast;
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
        VarArgsInfo varArgsInfo = new EqualTypesVarArgsBuilder(argumentsTypes).build();
        IBoundNode result = getVarArgsMethodNode(node, bindingContext, varArgsInfo);
        
        if (result == null) {
            varArgsInfo = new CastableTypesVarArgsBuilder(argumentsTypes, bindingContext).build();
            result = getVarArgsMethodNode(node, bindingContext, varArgsInfo);
        }
        
        return result; 
    }

   
    private IBoundNode getVarArgsMethodNode(ISyntaxNode node, IBindingContext bindingContext, VarArgsInfo varArgsInfo) {
        IMethodCaller methodWithLastArrayArgument = bindingContext.findMethodCaller(ISyntaxConstants.THIS_NAMESPACE, 
            methodName, varArgsInfo.getModifiedMethodArguments());
        
        // if can`t find the method, return null.
        //
        if (methodWithLastArrayArgument == null) {
            return null;
        }
        // get the component class of var arg expression
        //
        Class<?> componentVarArgClass = varArgsInfo.getVarArgClass().getInstanceClass();
        
        return new VariableArgumentsMethodBoundNode(node, children, methodWithLastArrayArgument, 
            varArgsInfo.getFirstVarArgIndex(), componentVarArgClass);
    }
    
    protected static interface VarArgsInfo {
        /** 
         * @return last index in the sequence of method arguments, that is equal to the last argument.<br>
         * E.g. if there are 3 argument types in sequence: <code>[StringOpenClass, IntOpenClass, IntOpenClass]</code>. The
         * result value will be 1.
         */
        int getFirstVarArgIndex();
        /**
         * The method is 'returnType foo(type param1, type param2)', but as there is no such method, parameters 
         * should be modified(wrapped) to array for further searching for method 'returnType foo(type[] params)'.
         * 
         * @return modified income arguments.
         */
        IOpenClass[] getModifiedMethodArguments();
        /**
         * Returns the component class of var arg expression
         * 
         * @return the component class of var arg expression
         */
        IOpenClass getVarArgClass();

    }
    
    protected static abstract class VarArgsBuilder {
        private final IOpenClass[] argumentsTypes;
        protected int firstVarArgIndex;
        protected IOpenClass varArgClass;
        protected IOpenClass[] modifiedMethodArguments;

        protected VarArgsBuilder(IOpenClass[] argumentsTypes) {
            this.argumentsTypes = argumentsTypes;
        }
        
        public VarArgsInfo build() {
            int numberOfAllArguments = argumentsTypes.length;
            // initialize the index of the first var arg value by the last index of
            // sequence
            //
            firstVarArgIndex = numberOfAllArguments - 1;
            
            varArgClass = argumentsTypes[numberOfAllArguments - 1];

            if (argumentsTypes.length >= 1) {
                for (int j = numberOfAllArguments - 2; j >= 0; j--) {
                    if (!ensureThatTypeIsVarArg(argumentsTypes[j])) {
                        // as the not equal arguments were found break
                        //
                        break;
                    }
                    // found previous argument of the same type as the last one
                    //
                    firstVarArgIndex = j;
                }
            }
            
            // list of modified method arguments types
            //
            List<IOpenClass> argumentsTypesSequence = new ArrayList<IOpenClass>();
            
            // the index of the first argument(from left to right) that is the member of var args expression.
            //
            if (firstVarArgIndex > 0) {
                // if there are arguments that cannot be put to the array of same type, just simply reuse them
                //
                for (int i = 0; i < firstVarArgIndex; i++) {
                    argumentsTypesSequence.add(argumentsTypes[i]);
                }
            }
            
            // add to the arguments type sequence the array type of the varArgClass.
            //
            argumentsTypesSequence.add(varArgClass.getAggregateInfo().getIndexedAggregateType(varArgClass, 1));
            
            modifiedMethodArguments = (IOpenClass[]) argumentsTypesSequence.toArray(new IOpenClass[argumentsTypesSequence.size()]);

            return createVarArgsInfo();
        }
        
        /**
         * Ensures that a "type" class can be used for var args.
         * Note that inside this method, a varArgClass field can be updated to a new value.
         * 
         * @param type checking type
         * @return true if a type is a var arg type, false otherwise
         */
        protected abstract boolean ensureThatTypeIsVarArg(IOpenClass type);

        private VarArgsInfo createVarArgsInfo() {
            return new VarArgsInfo() {
                @Override
                public int getFirstVarArgIndex() {
                    return firstVarArgIndex;
                }

                @Override
                public IOpenClass[] getModifiedMethodArguments() {
                    return modifiedMethodArguments;
                }

                @Override
                public IOpenClass getVarArgClass() {
                    return varArgClass;
                }
            };
        }
    }
    
    protected static class EqualTypesVarArgsBuilder extends VarArgsBuilder {

        protected EqualTypesVarArgsBuilder(IOpenClass[] argumentsTypes) {
            super(argumentsTypes);
        }

        @Override
        protected boolean ensureThatTypeIsVarArg(IOpenClass type) {
            return varArgClass.equals(type);
        }
    }

    protected static class CastableTypesVarArgsBuilder extends VarArgsBuilder {
        private final ICastFactory castFactory;

        protected CastableTypesVarArgsBuilder(IOpenClass[] argumentsTypes, ICastFactory castFactory) {
            super(argumentsTypes);
            this.castFactory = castFactory;
        }

        @Override
        protected boolean ensureThatTypeIsVarArg(IOpenClass type) {
            IOpenCast cast = castFactory.getCast(type, varArgClass);

            if (cast == null || !cast.isImplicit()) {
                cast = castFactory.getCast(varArgClass, type);
                if (cast == null || !cast.isImplicit()) {
                    // argument is not auto castable
                    return false;
                }
                
                // Arguments will be auto casted to this type
                varArgClass = type;
            }
            
            return true;
        }
        
    }
}
