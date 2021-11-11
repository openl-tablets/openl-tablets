/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 */
public class BinaryOperatorNodeBinder extends ANodeBinder {
    private static final Map<String, String> INVERSE_METHOD;

    static {
        Map<String, String> inverseMethod = new HashMap<>();
        inverseMethod.put("le", "ge");
        inverseMethod.put("lt", "gt");
        inverseMethod.put("ge", "le");
        inverseMethod.put("gt", "lt");
        inverseMethod.put("eq", "eq");
        inverseMethod.put("add", "add");
        INVERSE_METHOD = Collections.unmodifiableMap(inverseMethod);
    }

    public static IBoundNode bindOperator(ISyntaxNode node,
            String operatorName,
            IBoundNode b1,
            IBoundNode b2,
            IBindingContext bindingContext) {

        IOpenClass[] types = { b1.getType(), b2.getType() };
        IMethodCaller methodCaller = findBinaryOperatorMethodCaller(operatorName, types, bindingContext);

        if (methodCaller == null) {
            String message = errorMsg(operatorName, types[0], types[1]);
            return makeErrorNode(message, node, bindingContext);
        }

        return new BinaryOpNode(node, b1, b2, methodCaller);
    }

    public static String errorMsg(String methodName, IOpenClass t1, IOpenClass t2) {
        return String.format("Operator '%s(%s, %s)' is not found.", methodName, t1.getName(), t2.getName());
    }

    public static IMethodCaller findBinaryOperatorMethodCaller(String methodName,
            IOpenClass[] types,
            IBindingContext bindingContext) {

        IMethodCaller methodCaller = findSingleBinaryOperatorMethodCaller(methodName, types, bindingContext);

        if (methodCaller != null) {
            return methodCaller;
        }

        String inverse = INVERSE_METHOD.get(methodName);

        if (inverse != null) {

            IOpenClass[] invTypes = new IOpenClass[] { types[1], types[0] };
            methodCaller = findSingleBinaryOperatorMethodCaller(inverse, invTypes, bindingContext);

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
        IMethodCaller methodCaller = bindingContext
            .findMethodCaller(ISyntaxConstants.OPERATORS_NAMESPACE, methodName, argumentTypes);
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

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.INodeBinder#bind(org.openl.parser.ISyntaxNode, org.openl.env.IOpenEnv,
     * org.openl.binding.IBindingContext)
     */
    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        if (node.getNumberOfChildren() != 2) {
            throw SyntaxNodeExceptionUtils.createError("Binary node must have 2 subnodes.", node);
        }

        int index = node.getType().lastIndexOf('.');

        String methodName = node.getType().substring(index + 1);
        IBoundNode[] children = bindChildren(node, bindingContext);

        return bindOperator(node, methodName, children[0], children[1], bindingContext);
    }

}
