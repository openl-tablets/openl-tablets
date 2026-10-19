package org.openl.binding.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.MethodUtil;
import org.openl.binding.impl.cast.JavaNoCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * This the base class for a set of classes providing aggregate functions like SELECT FIRST, SELECT ALL, ORDER BY etc
 * <p>
 * An operator can name the element, as in {@code (x)} or {@code (Type x)}, and also the index of the element, as in
 * {@code (x, i)} or {@code (Type x, Integer i)}. The index is the position of the element from 0. It is an Integer,
 * and it can be declared of any type an Integer converts to without a loss, such as int, Long or Double.
 * <p>
 * Every operator skips the null elements of the array or collection. Its condition or expression is not evaluated
 * for them, and they are not in its result. A null result of an expression is not an element, so TRANSFORM TO keeps
 * it.
 * <p>
 * Every operator returns null for a missing array or collection.
 *
 * @author Yury Molchan
 */
public abstract class BaseAggregateIndexNodeBinder extends ANodeBinder {

    private static final IOpenClass INDEX_TYPE = JavaOpenClass.getOpenClass(Integer.class);

    /**
     * The number types that hold every Integer exactly.
     */
    private static final Set<Class<?>> WIDER_NUMBER_TYPES = Set
            .of(int.class, long.class, Long.class, double.class, Double.class, BigInteger.class, BigDecimal.class);

    @Override
    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        return makeErrorNode("This node always binds with target", node, bindingContext);
    }

    @Override
    public IBoundNode bindTarget(ISyntaxNode node, IBindingContext bindingContext, IBoundNode targetNode) {

        var containerType = targetNode.getType();

        if (NullOpenClass.the.equals(containerType)) {
            return makeErrorNode("An array or a collection is expected, but type '%s' is found.".formatted(
                    NullOpenClass.the.getName()), targetNode.getSyntaxNode(), bindingContext);
        }

        var info = containerType.getAggregateInfo();
        var componentType = info.getComponentType(containerType);
        if (componentType == null) {
            var typeName = containerType.getName();
            return makeErrorNode("An array or a collection is expected, but type '%s' is found.".formatted(
                    typeName), targetNode.getSyntaxNode(), bindingContext);
        }
        var numberOfChildren = node.getNumberOfChildren();
        if (numberOfChildren < 1 || numberOfChildren > 3) {
            return makeErrorNode("Aggregate node can have from 1 to 3 children nodes.", node, bindingContext);
        }

        try {
            bindingContext.pushLocalVarContext();
            var variables = declareVariables(node, containerType, componentType, bindingContext);
            if (variables == null) {
                return new ErrorBoundNode(node);
            }
            return bindExpression(node, targetNode, variables, bindingContext);
        } finally {
            bindingContext.popLocalVarContext();
        }
    }

    /**
     * Declares the variables of the operator in its local variable context.
     * <p>
     * The last syntax node is the expression. Without other nodes, the operator declares an element variable of its
     * own. Otherwise, the first node declares the element variable, and the second one declares the index variable.
     *
     * @return the variables, or {@code null} when a declaration has an error, which is already reported
     */
    private static @Nullable ElementVariables declareVariables(ISyntaxNode node,
                                                               IOpenClass containerType,
                                                               IOpenClass componentType,
                                                               IBindingContext bindingContext) {
        var numberOfChildren = node.getNumberOfChildren();
        if (numberOfChildren == 1) {
            var element = bindingContext
                    .addVar(ISyntaxConstants.THIS_NAMESPACE, bindingContext.getTemporaryVarName(), componentType);
            return new ElementVariables(element, JavaNoCast.getInstance(), List.of());
        }

        var container = MethodUtil.printType(containerType);
        var element = IndexParameterDeclarationBinder
                .declare(node.getChild(0), componentType, "Element of " + container, bindingContext);
        if (element == null) {
            return null;
        }
        var elementCast = bindingContext.getCast(componentType, element.getType());
        if (elementCast == null) {
            BindHelper.processError("Cannot cast '%s' to '%s'.".formatted(componentType, element.getType()),
                    element.getSyntaxNode(),
                    bindingContext);
            return null;
        }
        if (numberOfChildren == 2) {
            return new ElementVariables(element.getVariable(), elementCast, List.of(element));
        }

        var index = IndexParameterDeclarationBinder
                .declare(node.getChild(1), INDEX_TYPE, "Index of the element in " + container, bindingContext);
        if (index == null) {
            return null;
        }
        var indexCast = isIndexType(index.getType()) ? bindingContext.getCast(INDEX_TYPE, index.getType()) : null;
        if (indexCast == null) {
            BindHelper.processError("Cannot convert an index of type 'Integer' to type '%s'.".formatted(
                    index.getType().getName()), index.getSyntaxNode(), bindingContext);
            return null;
        }
        return new ElementVariables(element.getVariable(),
                elementCast,
                index.getVariable(),
                indexCast,
                List.of(element, index));
    }

    /**
     * Tells whether an Integer index converts to the type without a loss: a Java type an Integer is, such as Number,
     * or a number type that holds every Integer exactly, such as Long. An array, an alias type and a narrower number
     * type, such as short or float, do not hold it.
     */
    private static boolean isIndexType(IOpenClass type) {
        var instanceClass = type.getInstanceClass();
        return type instanceof JavaOpenClass && instanceClass != null && (instanceClass
                .isAssignableFrom(Integer.class) || WIDER_NUMBER_TYPES.contains(instanceClass));
    }

    /**
     * Binds the expression of the operator with its variables.
     */
    private IBoundNode bindExpression(ISyntaxNode node,
                                      IBoundNode targetNode,
                                      ElementVariables variables,
                                      IBindingContext bindingContext) {
        var boundExpressionNode = bindChildNode(node.getChild(node.getNumberOfChildren() - 1),
                TypeBindingContext.create(bindingContext, variables.element(), 1, variables.index()));
        if (boundExpressionNode instanceof TypeBoundNode) {
            var message = "Type definition cannot be used as expression for array index operator.";
            return makeErrorNode(message, boundExpressionNode.getSyntaxNode(), bindingContext);
        }
        return createBoundNode(node, targetNode, boundExpressionNode, variables, bindingContext);
    }

    abstract IBoundNode createBoundNode(ISyntaxNode node,
                                        IBoundNode targetNode,
                                        IBoundNode expressionNode,
                                        ElementVariables variables,
                                        IBindingContext bindingContext);
}
