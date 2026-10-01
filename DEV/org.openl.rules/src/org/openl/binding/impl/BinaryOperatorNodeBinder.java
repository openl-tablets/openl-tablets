/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.lang.reflect.Modifier;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.domain.IDomain;
import org.openl.rules.operator.Comparison;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.CastingMethodCaller;
import org.openl.util.OpenClassUtils;

/**
 * @author snshor
 */
public class BinaryOperatorNodeBinder extends ANodeBinder {
    private static final Map<String, String> INVERSE_METHOD;

    static {
        INVERSE_METHOD = Map.of("le", "ge", "lt", "gt", "ge", "le", "gt", "lt", "eq", "eq", "add", "add");
    }

    public static IBoundNode bindOperator(ISyntaxNode node,
                                          String operatorName,
                                          IBoundNode b1,
                                          IBoundNode b2,
                                          IBindingContext bindingContext) {

        IOpenClass[] types = {b1.getType(), b2.getType()};
        IMethodCaller methodCaller = findOperatorMethodCaller(operatorName, types, bindingContext);
        if (methodCaller == null) {
            String message = errorMsg(operatorName, types[0], types[1]);
            return makeErrorNode(message, node, bindingContext);
        }

        if (methodCaller instanceof CastingMethodCaller) {
            var method = methodCaller.getMethod();
            if (("eq".equals(method.getName()) || "ne".equals(method.getName())) && method.getDeclaringClass()
                    .getInstanceClass() == Comparison.class) {
                var parameterTypes = method.getSignature().getParameterTypes();
                if (parameterTypes.length == 2) {
                    if (parameterTypes[0].getInstanceClass() == Object.class && parameterTypes[1]
                            .getInstanceClass() == Object.class) {
                        validateComparisonWithObjectIncluded(b1, b2, method, node, bindingContext);
                    }
                    validateComparisonLiteralWithDomainType(b1, b2, method, node, bindingContext);
                }
            }
        }

        return new BinaryOpNode(node, b1, b2, methodCaller);
    }

    private static void validateComparisonLiteralWithDomainType(IBoundNode b1,
                                                                IBoundNode b2,
                                                                IOpenMethod method,
                                                                ISyntaxNode node,
                                                                IBindingContext bindingContext) {
        validateComparisonLiteralWithDomainTypeInternal(b1, b2, method, node, bindingContext);
        validateComparisonLiteralWithDomainTypeInternal(b2, b1, method, node, bindingContext);
    }

    @SuppressWarnings("unchecked")
    private static void validateComparisonLiteralWithDomainTypeInternal(IBoundNode domainNode,
                                                                        IBoundNode literalNode,
                                                                        IOpenMethod method,
                                                                        ISyntaxNode node,
                                                                        IBindingContext bindingContext) {
        if (domainNode.getType().getDomain() != null && literalNode instanceof LiteralBoundNode literalBoundNode) {
            var domain = (IDomain<Object>) domainNode.getType().getDomain();
            if (literalBoundNode.getValue() != null && !domain.selectObject(literalBoundNode.getValue())) {
                BindHelper.processWarn("Warning: Object '%s' is outside of valid domain '%s'. The comparison always returns %s.".formatted(
                        literalBoundNode.getValue(),
                        domainNode.getType().getName(),
                        "ne".equals(method.getName())), node, bindingContext);
            }
        }
    }

    private static void validateComparisonWithObjectIncluded(IBoundNode b1,
                                                             IBoundNode b2,
                                                             IOpenMethod method,
                                                             ISyntaxNode node,
                                                             IBindingContext bindingContext) {
        var b1Type = b1.getType();
        var b2Type = b2.getType();
        if (b1Type == null || b2Type == null || b1Type.equals(b2Type)) {
            return;
        }
        Class<?> b1InstanceClass = b1Type.getInstanceClass();
        Class<?> b2InstanceClass = b2Type.getInstanceClass();
        if (b1InstanceClass == null || b2InstanceClass == null) {
            return;
        }
        var b1Final = Modifier.isFinal(b1InstanceClass.getModifiers());
        var b2Final = Modifier.isFinal(b2InstanceClass.getModifiers());
        if (!b1Final && !b2Final) {
            return;
        }
        if (b1Final && b2InstanceClass.isAssignableFrom(b1InstanceClass)) {
            return;
        }
        if (b2Final && b1InstanceClass.isAssignableFrom(b2InstanceClass)) {
            return;
        }
        BindHelper.processWarn("Warning: Compared elements have different types ('%s', '%s'). Comparing these types always returns %s.".formatted(
                b1Type.getName(),
                b2Type.getName(),
                "ne".equals(method.getName())), node, bindingContext);
    }

    public static String errorMsg(String methodName, IOpenClass t1, IOpenClass t2) {
        return "Operator '%s(%s, %s)' is not found.".formatted(methodName, t1.getName(), t2.getName());
    }

    /**
     * Finds the operator for the types of the operands. The null literal is a value of the type of the other operand,
     * boxed when that type is primitive. So {@code null + 3} adds two integers, as a null {@code Integer} variable plus
     * 3 does, instead of adding days to a null date.
     *
     * <p>When no operator accepts the null literal as that type, the operator is searched for the null literal as it
     * is, so {@code null + true} still concatenates strings.
     */
    private static @Nullable IMethodCaller findOperatorMethodCaller(String methodName,
                                                                    IOpenClass[] types,
                                                                    IBindingContext bindingContext) {
        IOpenClass[] typedNull = {typeNull(types[0], types[1]), typeNull(types[1], types[0])};
        IMethodCaller methodCaller = null;
        if (typedNull[0] != types[0] || typedNull[1] != types[1]) {
            methodCaller = findBinaryOperatorMethodCaller(methodName, typedNull, bindingContext);
        }
        return methodCaller != null ? methodCaller
                : findBinaryOperatorMethodCaller(methodName, types, bindingContext);
    }

    /**
     * Returns the type of the other operand, boxed when it is primitive, for the null literal, and the type itself
     * for any other operand.
     */
    private static IOpenClass typeNull(IOpenClass type, IOpenClass otherType) {
        return NullOpenClass.isAnyNull(type) ? OpenClassUtils.toWrapperIfPrimitive(otherType) : type;
    }

    public static IMethodCaller findBinaryOperatorMethodCaller(String methodName,
                                                               IOpenClass[] types,
                                                               IBindingContext bindingContext) {

        IMethodCaller methodCaller = findSingleBinaryOperatorMethodCaller(methodName, types, bindingContext);

        if (methodCaller != null) {
            return methodCaller;
        }

        var inverse = INVERSE_METHOD.get(methodName);

        if (inverse != null) {

            IOpenClass[] invTypes = new IOpenClass[]{types[1], types[0]};
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
        var methodCaller = bindingContext
                .findMethodCaller(ISyntaxConstants.OPERATORS_NAMESPACE, methodName, argumentTypes);
        if (methodCaller != null) {
            return methodCaller;
        }

        IOpenClass[] types2 = {argumentTypes[1]};

        // An attempt to find method <methodName>(argumentTypes[1]), using the first argument type as a possible
        // collection of suitable methods.
        //
        // TODO: Investigate which case covers this branch. How the method, e.g. foo(Type2) may be suitable
        // for foo(Type1, Type2). Why it has more priority than next items for search?
        // @author DLiauchuk
        //
        methodCaller = MethodSearch.findMethod(methodName, types2, bindingContext, argumentTypes[0], false);
        if (methodCaller != null) {
            return methodCaller;
        }

        // An attempt to find method <methodName>(argumentTypes), using the first argument type as a possible
        // collection of suitable methods, e.g. {@link DoubleValue#add(DoubleValue value1, DoubleValue value2).
        //
        methodCaller = MethodSearch.findMethod(methodName, argumentTypes, bindingContext, argumentTypes[0], false);
        if (methodCaller != null) {
            return methodCaller;
        }

        // An attempt to find method <methodName>(argumentTypes), using the second argument type as a possible
        // collection of suitable methods.
        //
        return MethodSearch.findMethod(methodName, argumentTypes, bindingContext, argumentTypes[1], false);
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

        var index = node.getType().lastIndexOf('.');

        var methodName = node.getType().substring(index + 1);
        IBoundNode[] children = bindChildren(node, bindingContext);

        return bindOperator(node, methodName, children[0], children[1], bindingContext);
    }

}
