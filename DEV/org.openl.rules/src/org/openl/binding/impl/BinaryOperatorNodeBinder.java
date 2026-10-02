/*
 * Created on May 19, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.binding.impl.cast.IOpenCast;
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
import org.openl.types.java.JavaOpenClass;
import org.openl.util.OpenClassUtils;

/**
 * @author snshor
 */
public class BinaryOperatorNodeBinder extends ANodeBinder {
    private static final Map<String, String> INVERSE_METHOD;

    static {
        INVERSE_METHOD = Map.of("le", "ge", "lt", "gt", "ge", "le", "gt", "lt", "eq", "eq", "add", "add");
    }

    private static final Set<String> STRICT_OPERATORS = Set
            .of("strict_eq", "strict_ne", "strict_lt", "strict_gt", "strict_le", "strict_ge");

    private static final Set<Class<?>> NUMBERS = Set.of(byte.class,
            short.class,
            int.class,
            long.class,
            float.class,
            double.class,
            Byte.class,
            Short.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            BigInteger.class,
            BigDecimal.class);

    private static final Set<Class<?>> FLOATING_POINT_NUMBERS = Set
            .of(float.class, double.class, Float.class, Double.class, BigDecimal.class);

    private static final Set<Class<?>> WIDER_THAN_FLOAT = Set
            .of(double.class, Double.class, BigDecimal.class, BigInteger.class);

    private static final Set<Class<?>> DOUBLES = Set.of(double.class, Double.class);

    public static IBoundNode bindOperator(ISyntaxNode node,
                                          String operatorName,
                                          IBoundNode operand1,
                                          IBoundNode operand2,
                                          IBindingContext bindingContext) {

        var operands = STRICT_OPERATORS.contains(operatorName)
                ? strictOperands(operand1, operand2, bindingContext)
                : new Operands(operand1, operand2);
        var b1 = operands.left();
        var b2 = operands.right();
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

    private record Operands(IBoundNode left, IBoundNode right) {
    }

    /**
     * Prepares the operands of a strict comparison. Numbers of different types, one of them a floating point number,
     * are converted to one type, so that they are compared by value.
     *
     * <p>A float compared with a double, a BigDecimal or a BigInteger keeps its binary value, as Java widens it. So
     * 4.3f is compared as 4.300000190734863, not as the double 4.3 that other operators make of it. The other numbers
     * are converted as in the not strict operators: 4 becomes 4.0 next to a double, and a BigInteger becomes a
     * BigDecimal. A double stays a double next to a BigDecimal, because the operators of the two compare NaN and the
     * infinities, which no BigDecimal holds.
     *
     * <p>Any other operands are returned as they are.
     */
    private static Operands strictOperands(IBoundNode operand1, IBoundNode operand2, IBindingContext bindingContext) {
        var left = widenFloat(operand1, operand2.getType());
        var right = widenFloat(operand2, operand1.getType());
        var leftType = left.getType();
        var rightType = right.getType();
        if (isIn(NUMBERS, leftType) && isIn(NUMBERS, rightType)
                && (isIn(FLOATING_POINT_NUMBERS, leftType) || isIn(FLOATING_POINT_NUMBERS, rightType))) {
            var wider = CastToWiderType.create(bindingContext, leftType, rightType);
            var widerType = wider.getWiderType();
            var decimal = widerType != null && widerType.getInstanceClass() == BigDecimal.class;
            left = decimal && isIn(DOUBLES, leftType) ? left : wider.castFirst(left);
            right = decimal && isIn(DOUBLES, rightType) ? right : wider.castSecond(right);
        }
        return new Operands(left, right);
    }

    /**
     * Returns a float operand as a double with the same binary value when the other operand is a double, a BigDecimal
     * or a BigInteger. Any other operand is returned as it is.
     */
    private static IBoundNode widenFloat(IBoundNode operand, @Nullable IOpenClass otherType) {
        var type = operand.getType() == null ? null : operand.getType().getInstanceClass();
        if ((type == float.class || type == Float.class) && isIn(WIDER_THAN_FLOAT, otherType)) {
            var doubleType = type == float.class ? JavaOpenClass.DOUBLE : JavaOpenClass.getOpenClass(Double.class);
            return new CastNode(null, operand, BinaryFloatCast.INSTANCE, doubleType);
        }
        return operand;
    }

    private static boolean isIn(Set<Class<?>> classes, @Nullable IOpenClass type) {
        var instanceClass = type == null ? null : type.getInstanceClass();
        return instanceClass != null && classes.contains(instanceClass);
    }

    /**
     * Converts a float to the double with the same binary value. An absent value stays absent.
     */
    private enum BinaryFloatCast implements IOpenCast {
        INSTANCE;

        @Override
        public @Nullable Object convert(@Nullable Object from) {
            return from == null ? null : ((Number) from).doubleValue();
        }

        @Override
        public int getDistance() {
            return CastFactory.PRIMITIVE_TO_PRIMITIVE_AUTOCAST_DISTANCE;
        }

        @Override
        public boolean isImplicit() {
            return true;
        }
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
        // collection of suitable methods. Such a method is an instance method of the first operand, which
        // BinaryOpNode invokes on it, e.g. BigDecimal.add(BigDecimal) for a + b.
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
