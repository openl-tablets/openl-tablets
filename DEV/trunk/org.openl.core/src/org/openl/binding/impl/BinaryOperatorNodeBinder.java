/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;

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
            String message = errorMsg(operatorName, types[0], types[1]);
            BindHelper.processError(message, node, bindingContext, false);

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
                                                                      IOpenClass[] types,
                                                                      IBindingContext bindingContext) {

        IMethodCaller methodCaller = bindingContext.findMethodCaller(ISyntaxConstants.OPERATORS_NAMESPACE, methodName, types);

        if (methodCaller != null) {
            return methodCaller;
        }

        IOpenClass[] types2 = { types[1] };

        methodCaller = MethodSearch.getMethodCaller(methodName, types2, bindingContext, types[0]);

        if (methodCaller != null) {
            return methodCaller;
        }

        methodCaller = MethodSearch.getMethodCaller(methodName, types, bindingContext, types[0]);

        if (methodCaller != null) {
            return methodCaller;
        }

        methodCaller = MethodSearch.getMethodCaller(methodName, types, bindingContext, types[1]);

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
            throw new SyntaxNodeException("Binary node must have 2 subnodes", null, node);
        }

        int index = node.getType().lastIndexOf('.');

        String methodName = node.getType().substring(index + 1);
        IBoundNode[] children = bindChildren(node, bindingContext);

        return bindOperator(node, methodName, children[0], children[1], bindingContext);
    }

}
