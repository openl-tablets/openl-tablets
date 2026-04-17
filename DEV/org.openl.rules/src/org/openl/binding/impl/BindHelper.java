package org.openl.binding.impl;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.binding.impl.method.AOpenMethodDelegator;
import org.openl.domain.EnumDomain;
import org.openl.message.OpenLMessagesUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.ArrayLengthOpenField;
import org.openl.types.impl.CastingMethodCaller;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.types.java.JavaOpenConstructor;
import org.openl.types.java.JavaOpenField;
import org.openl.types.java.JavaOpenMethod;
import org.openl.util.DomainUtils;
import org.openl.util.OpenClassUtils;

public final class BindHelper {

    private BindHelper() {
    }

    public static void processError(Throwable error, IBindingContext bindingContext) {
        SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils.createError(error, null);
        bindingContext.addError(syntaxNodeException);
    }

    public static void processError(String message, ISyntaxNode syntaxNode, IBindingContext bindingContext) {
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, syntaxNode);
        bindingContext.addError(error);
    }

    public static void processError(String message,
                                    Throwable ex,
                                    ISyntaxNode syntaxNode,
                                    IBindingContext bindingContext) {
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, ex, syntaxNode);
        bindingContext.addError(error);
    }

    public static void processError(String message, IOpenSourceCodeModule source, IBindingContext bindingContext) {
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, source);
        bindingContext.addError(error);
    }

    public static void processError(String message,
                                    Throwable ex,
                                    IOpenSourceCodeModule source,
                                    IBindingContext bindingContext) {
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, ex, null, source);
        bindingContext.addError(error);
    }

    public static void processError(Throwable ex, IOpenSourceCodeModule source, IBindingContext bindingContext) {
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(ex, null, source);
        bindingContext.addError(error);
    }

    public static void processError(Throwable e, ISyntaxNode node, IBindingContext bindingContext) {
        SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils.createError(e, node);
        bindingContext.addError(syntaxNodeException);
    }

    private static final Collection<String> EQUAL_OPERATORS = Collections
            .unmodifiableCollection(Arrays.asList("op.binary.eq",
                    "op.binary.strict_eq",
                    "op.binary.le",
                    "op.binary.strict_le",
                    "op.binary.ge",
                    "op.binary.strict_ge"));
    private static final Collection<String> NOT_EQUAL_OPERATORS = Collections
            .unmodifiableCollection(Arrays.asList("op.binary.ne",
                    "op.binary.strict_ne",
                    "op.binary.lt",
                    "op.binary.strict_lt",
                    "op.binary.gt",
                    "op.binary.strict_gt"));

    /**
     * Checks the condition expression.
     *
     * @param conditionNode  Bound node that represents condition expression.
     * @param bindingContext Binding context.
     * @return <code>conditionNode</code> in case it is correct, and {@link ErrorBoundNode} in case condition is wrong.
     */
    public static IBoundNode checkConditionBoundNode(IBoundNode conditionNode, IBindingContext bindingContext) {
        if (conditionNode != null && !isBooleanType(conditionNode.getType())) {
            if (conditionNode.getType() != NullOpenClass.the) {
                BindHelper.processError("Expected boolean type for condition.",
                        conditionNode.getSyntaxNode(),
                        bindingContext);
            }
            return new ErrorBoundNode(conditionNode.getSyntaxNode());
        } else {
            if (conditionNode != null) {
                checkForSameLeftAndRightExpression(conditionNode, bindingContext);
            }

            return conditionNode;
        }
    }

    public static void checkOnDeprecation(ISyntaxNode node, IBindingContext context, IMethodCaller caller) {
        if (caller instanceof JavaOpenMethod method) {
            Method javaMethod = method.getJavaMethod();
            if (isDeprecated(javaMethod)) {
                String msg = "DEPRECATED '%s' function will be removed in the next version.".formatted(
                        javaMethod.getName());
                processWarn(msg, node, context);
            }
        } else if (caller instanceof JavaOpenConstructor constructor) {
            Constructor<?> constr = constructor.getJavaConstructor();
            if (isDeprecated(constr)) {
                String msg = "DEPRECATED '%s' constructor will be removed in the next version.".formatted(
                        constr.getName());
                processWarn(msg, node, context);
            }
        } else if (caller instanceof CastingMethodCaller) {
            checkOnDeprecation(node, context, caller.getMethod());
        } else if (caller instanceof AOpenMethodDelegator delegator) {
            checkOnDeprecation(node, context, delegator.getDelegate());
        }
    }

    public static void checkOnDeprecation(ISyntaxNode node, IBindingContext context, IOpenClass aClass) {
        if (aClass instanceof JavaOpenClass) {
            Class<?> javaClass = aClass.getInstanceClass();
            if (javaClass.isAnnotationPresent(Deprecated.class)) {
                String msg = "DEPRECATED '%s' class will be removed in the next version.".formatted(
                        javaClass.getTypeName());
                processWarn(msg, node, context);
            }
        } else if (aClass != null && aClass.isArray()) {
            checkOnDeprecation(node, context, aClass.getComponentClass());
        }
    }

    public static void checkOnDeprecation(ISyntaxNode node, IBindingContext context, IOpenField field) {
        if (field instanceof JavaOpenField openField) {
            Field javaField = openField.getJavaField();
            if (isDeprecated(javaField)) {
                String msg = "DEPRECATED '%s' field will be removed in the next version.".formatted(
                        javaField.getName());
                processWarn(msg, node, context);
            }
        } else if (field instanceof ArrayLengthOpenField) {
            processWarn(
                    "DEPRECATED 'length' field for arrays will be removed in the next version. " + "Use length() function instead.",
                    node,
                    context);
        }
    }

    private static <T extends Member & AnnotatedElement> boolean isDeprecated(T javaField) {
        return javaField.isAnnotationPresent(Deprecated.class) || javaField.getDeclaringClass()
                .isAnnotationPresent(Deprecated.class);
    }

    private static void checkForSameLeftAndRightExpression(IBoundNode conditionNode, IBindingContext bindingContext) {
        IBoundNode[] children = conditionNode.getChildren();
        if (children != null && children.length == 2) {
            IBoundNode left = children[0];
            IBoundNode right = children[1];
            if (isSame(left, right)) {
                String type = conditionNode.getSyntaxNode().getType();

                if (EQUAL_OPERATORS.contains(type)) {
                    BindHelper.processWarn("Condition is always true.", conditionNode.getSyntaxNode(), bindingContext);
                } else if (NOT_EQUAL_OPERATORS.contains(type)) {
                    BindHelper.processWarn("Condition is always false.", conditionNode.getSyntaxNode(), bindingContext);
                }
            }
        }
    }

    private static boolean isSame(IBoundNode left, IBoundNode right) {
        if (left instanceof FieldBoundNode node2 && right instanceof FieldBoundNode node3) {
            if (node2.getBoundField() == node3.getBoundField()) {
                if (left.getTargetNode() == right.getTargetNode()) {
                    return true;
                }
                return isSame(left.getTargetNode(), right.getTargetNode());
            }
        } else if (left instanceof LiteralBoundNode node && right instanceof LiteralBoundNode node1) {
            Object leftValue = node.getValue();
            Object rightValue = node1.getValue();
            return Objects.equals(leftValue, rightValue);
        }

        return false;
    }

    public static void processWarn(String message, ISyntaxNode source, IBindingContext bindingContext) {
        bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(message, source));
    }

    /**
     * Checks given type that it is boolean type.
     *
     * @param type {@link IOpenClass} instance
     * @return <code>true</code> if given type equals {@link JavaOpenClass#BOOLEAN} or
     * JavaOpenClass.getOpenClass(Boolean.class); otherwise - <code>false</code>
     */
    private static boolean isBooleanType(IOpenClass type) {
        return type == null || JavaOpenClass.BOOLEAN == type || JavaOpenClass.getOpenClass(Boolean.class) == type;
    }

    /**
     * Validates a bound node against a domain type at compile time.
     * Only literal expressions (literals, arrays of literals, casts around literals) are validated.
     * Method calls and field accesses are skipped as their values are only known at runtime.
     */
    public static void validateDomainValue(IBoundNode boundNode,
                                           IOpenClass type,
                                           IBindingContext bindingContext) {
        if (type instanceof DomainOpenClass && isLiteralExpression(boundNode)) {
            if (type.isArray()) {
                validateForArrayDomain(boundNode, type, bindingContext);
            } else {
                validateForScalarDomain(boundNode, type, bindingContext);
            }
        }
    }

    private static boolean isLiteralExpression(IBoundNode node) {
        if (node instanceof LiteralBoundNode) {
            return true;
        }
        if (isTransparentNode(node)) {
            IBoundNode[] children = node.getChildren();
            if (children != null) {
                for (IBoundNode child : children) {
                    if (isLiteralExpression(child)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Node types that are data-structure wrappers and safe to recurse through for validation.
     * Method calls, field accesses, and binary operations are NOT safe — they are runtime-computed.
     */
    private static boolean isTransparentNode(IBoundNode node) {
        return node instanceof ArrayInitializerNode
                || node instanceof CastNode
                || node instanceof BlockNode;
    }

    private static void validateForScalarDomain(IBoundNode node,
                                                IOpenClass parameterType,
                                                IBindingContext bindingContext) {
        if (node instanceof LiteralBoundNode literalBoundNode) {
            validateLiteralAgainstDomain(literalBoundNode, parameterType, bindingContext);
        } else if (isTransparentNode(node) && node.getChildren() != null) {
            for (IBoundNode child : node.getChildren()) {
                validateForScalarDomain(child, parameterType, bindingContext);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void validateForArrayDomain(IBoundNode node,
                                               IOpenClass parameterType,
                                               IBindingContext bindingContext) {
        var domain = parameterType.getDomain();
        if (!(domain instanceof EnumDomain<?> enumDomain)) {
            return;
        }
        var allObjects = ((EnumDomain<Object>) enumDomain).getAllObjects();
        validateNodeAgainstArrayDomain(node, allObjects, parameterType, bindingContext);
    }

    private static void validateNodeAgainstArrayDomain(IBoundNode node,
                                                       Object[] allObjects,
                                                       IOpenClass parameterType,
                                                       IBindingContext bindingContext) {
        if (node instanceof LiteralBoundNode literalBoundNode) {
            if (literalBoundNode.getValue() != null) {
                validateKeyAgainstArrayDomain(literalBoundNode.getValue().toString(),
                        allObjects, parameterType, node, bindingContext);
            }
            return;
        }
        IBoundNode[] children = node.getChildren();
        if (children == null) {
            return;
        }
        for (IBoundNode child : children) {
            validateNodeAgainstArrayDomain(child, allObjects, parameterType, bindingContext);
        }
    }

    private static void validateKeyAgainstArrayDomain(String key,
                                                      Object[] allObjects,
                                                      IOpenClass parameterType,
                                                      IBoundNode node,
                                                      IBindingContext bindingContext) {
        if (!OpenClassUtils.belongsToEnum(allObjects, key)) {
            processError(
                    String.format("Object '%s' is outside of valid domain '%s'. Valid values: %s",
                            key,
                            parameterType.getName(),
                            DomainUtils.toString(parameterType.getDomain())),
                    node.getSyntaxNode(),
                    bindingContext);
        }
    }

    private static void validateLiteralAgainstDomain(LiteralBoundNode literalNode,
                                                     IOpenClass parameterType,
                                                     IBindingContext bindingContext) {
        IOpenClass fromType = literalNode.getType();
        if (fromType == null || fromType.equals(parameterType)) {
            return;
        }
        IOpenCast cast = bindingContext.getCast(fromType, parameterType);
        if (cast == null) {
            return;
        }
        try {
            cast.convert(literalNode.getValue());
        } catch (OutsideOfValidDomainException exception) {
            processError(
                    String.format("Object '%s' is outside of valid domain '%s'. Valid values: %s",
                            literalNode.getValue(),
                            parameterType.getName(),
                            DomainUtils.toString(parameterType.getDomain())),
                    literalNode.getSyntaxNode(),
                    bindingContext);
        }
    }

}
