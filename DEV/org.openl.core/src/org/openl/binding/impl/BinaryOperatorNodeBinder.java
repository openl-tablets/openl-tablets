/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;

/**
 * @author snshor
 */
public class BinaryOperatorNodeBinder extends ANodeBinder {

    public static IBoundNode bindOperator(ISyntaxNode node,
                                          String operatorName,
                                          IBoundNode b1,
                                          IBoundNode b2,
                                          IBindingContext bindingContext) throws SyntaxNodeException {

        IOpenClass[] types = { b1.getType(), b2.getType() };
        IMethodCaller methodCaller = findBinaryOperatorMethodCaller(operatorName, types, bindingContext);

        if (methodCaller == null) {
        	if (!NullOpenClass.isAnyNull(types)){
        		String message = errorMsg(operatorName, types[0], types[1]);
        		BindHelper.processError(message, node, bindingContext, false);
        	}	

            return new ErrorBoundNode(node);
        }

        return new BinaryOpNode(node, new IBoundNode[] { b1, b2 }, methodCaller);
    }

    public static String errorMsg(String methodName, IOpenClass t1, IOpenClass t2) {
        return "Operator not defined for: " + methodName + "(" + t1.getName() + ", " + t2.getName() + ")";
    }

    public static IMethodCaller findBinaryOperatorMethodCaller(String methodName,
                                                               IOpenClass[] types,
                                                               IBindingContext bindingContext) {

        IMethodCaller methodCaller = findSingleBinaryOperatorMethodCaller(methodName, types, bindingContext);

        if (methodCaller != null) {
            return methodCaller;
        }

        BinaryOperatorMap binaryOperations = BinaryOperatorMap.findOp(methodName);

        if (binaryOperations == null) {
            return methodCaller;
        }

        methodCaller = findWithSynonims(methodName, types, bindingContext, binaryOperations.getSynonims());

        if (methodCaller != null) {
            return methodCaller;
        }

        if (binaryOperations.isSymmetrical()) {

            IOpenClass[] symTypes = new IOpenClass[] { types[1], types[0] };
            methodCaller = findSingleBinaryOperatorMethodCaller(methodName, symTypes, bindingContext);

            if (methodCaller != null) {
                return new BinaryMethodCallerSwapParams(methodCaller);
            }

            methodCaller = findWithSynonims(methodName, symTypes, bindingContext, binaryOperations.getSynonims());

            if (methodCaller != null) {
                return new BinaryMethodCallerSwapParams(methodCaller);
            }
        }

        if (binaryOperations.getInverse() != null) {

            IOpenClass[] invTypes = new IOpenClass[] { types[1], types[0] };
            methodCaller = findSingleBinaryOperatorMethodCaller(binaryOperations.getInverse(), invTypes, bindingContext);

            if (methodCaller != null) {
                return new BinaryMethodCallerSwapParams(methodCaller);
            }

            BinaryOperatorMap bopInv = BinaryOperatorMap.findOp(binaryOperations.getInverse());
            methodCaller = findWithSynonims(methodName, invTypes, bindingContext, bopInv.getSynonims());

            if (methodCaller != null) {
                return new BinaryMethodCallerSwapParams(methodCaller);
            }
        }

        return null;
    }

    private static IMethodCaller findSingleBinaryOperatorMethodCaller(String methodName,
                                                                      IOpenClass[] argumentTypes,
                                                                      IBindingContext bindingContext) {
        
        // An attempt to find the method <namespace>.<methodName>(argumentTypes) in the binding context.
        // This is the most privileged place for searching.
        // @author DLiauchuk
        //
        IMethodCaller methodCaller = bindingContext.findMethodCaller(ISyntaxConstants.OPERATORS_NAMESPACE, methodName, 
            argumentTypes);
        if (methodCaller != null) {
            return methodCaller;
        }

        IOpenClass[] types2 = { argumentTypes[1] };
        
        // An attempt to find method <methodName>(argumentTypes[1]), using the first argument type as a possible 
        // collection of suitable methods.
        //
        // TODO: Investigate which case covers this branch. How the method, e.g. foo(Type2) may be suitable
        // for foo(Type1, Type2). Why it has more priority than next items for search?
        // @author DLiauchuk
        //
        methodCaller = MethodSearch.findMethod(methodName, types2, bindingContext, argumentTypes[0]);
        if (methodCaller != null) {
            return methodCaller;
        }
        
        // An attempt to find method <methodName>(argumentTypes), using the first argument type as a possible
        // collection of suitable methods, e.g. {@link DoubleValue#add(DoubleValue value1, DoubleValue value2).
        //
        methodCaller = MethodSearch.findMethod(methodName, argumentTypes, bindingContext, argumentTypes[0]);
        if (methodCaller != null) {
            return methodCaller;
        }
        
        // An attempt to find method <methodName>(argumentTypes), using the second argument type as a possible
        // collection of suitable methods.
        //
        methodCaller = MethodSearch.findMethod(methodName, argumentTypes, bindingContext, argumentTypes[1]);

        return methodCaller;
    }

    private static IMethodCaller findWithSynonims(String methodName,
                                                  IOpenClass[] types,
                                                  IBindingContext bindingContext,
                                                  String[] synonims) {

        IMethodCaller methodCaller = null;

        if (synonims != null) {
            for (int i = 0; i < synonims.length; i++) {

                methodCaller = findSingleBinaryOperatorMethodCaller(synonims[i], types, bindingContext);

                if (methodCaller != null) {
                    return methodCaller;
                }
            }
        }

        return methodCaller;
    }

    /*
     * (non-Javadoc)
     * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv,
     * org.openl.binding.IBindingContext)
     */
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        if (node.getNumberOfChildren() != 2) {
            throw SyntaxNodeExceptionUtils.createError("Binary node must have 2 subnodes", node);
        }

        int index = node.getType().lastIndexOf('.');

        String methodName = node.getType().substring(index + 1);
        IBoundNode[] children = bindChildren(node, bindingContext);

        return bindOperator(node, methodName, children[0], children[1], bindingContext);
    }

}
