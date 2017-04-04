package org.openl.binding.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundNode;
import org.openl.message.OpenLMessagesUtils;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;

public class BindHelper {

	private BindHelper() {
	}

	public static void processError(ISyntaxNode syntaxNode,
			Throwable throwable, IBindingContext bindingContext) {

		SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(
				throwable, syntaxNode);
		processError(error, bindingContext);
	}

	public static void processError(String message, ISyntaxNode syntaxNode,
			Throwable throwable, IBindingContext bindingContext) {

		SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(
				message, throwable, syntaxNode);
		processError(error, bindingContext);
	}

	public static void processError(CompositeSyntaxNodeException error,
			IBindingContext bindingContext) {
		SyntaxNodeException[] errors = error.getErrors();
		for (SyntaxNodeException e : errors) {
			processError(e, bindingContext);
		}
	}

	public static void processError(SyntaxNodeException error,
			IBindingContext bindingContext) {

		bindingContext.addError(error);
		processError(error);
	}

	public static void processError(Throwable error, ISyntaxNode syntaxNode,
			IBindingContext bindingContext) {
		processError(error, syntaxNode, bindingContext, true);
	}

	public static void processError(Throwable error, ISyntaxNode syntaxNode,
			IBindingContext bindingContext, boolean storeGlobal) {
		SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils
				.createError(error, syntaxNode);
		processSyntaxNodeException(syntaxNodeException, storeGlobal,
				bindingContext);
	}

	public static void processError(String message, ISyntaxNode syntaxNode,
			IBindingContext bindingContext) {
		processError(message, syntaxNode, bindingContext, true);
	}

	public static void processError(String message, ISyntaxNode syntaxNode,
			IBindingContext bindingContext, boolean storeGlobal) {
		SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(
				message, syntaxNode);
		processSyntaxNodeException(error, storeGlobal, bindingContext);
	}

	private static void processSyntaxNodeException(SyntaxNodeException error,
			boolean storeGlobal, IBindingContext bindingContext) {
		bindingContext.addError(error);
		if (storeGlobal) {
			processError(error);
		}
	}

	public static void processError(String message, ISyntaxNode syntaxNode,
			Throwable throwable) {

		SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(
				message, throwable, syntaxNode);
		processError(error);
	}

	public static void processError(String message, ISyntaxNode syntaxNode) {

		SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(
				message, syntaxNode);
		processError(error);
	}

	public static void processError(SyntaxNodeException error) {
		OpenLMessagesUtils.addError(error);
	}

	public static final String CONDITION_TYPE_MESSAGE = "Condition must have boolean type";
	private static final Collection<String> EQUAL_OPERATORS = Collections.unmodifiableCollection(Arrays.asList(
			"op.binary.eq",
			"op.binary.strict_eq",
			"op.binary.le",
			"op.binary.strict_le",
			"op.binary.ge",
			"op.binary.strict_ge"
	));
	private static final Collection<String> NOT_EQUAL_OPERATORS = Collections.unmodifiableCollection(Arrays.asList(
			"op.binary.ne",
			"op.binary.strict_ne",
			"op.binary.lt",
			"op.binary.strict_lt",
			"op.binary.gt",
			"op.binary.strict_gt"
	));

	/**
	 * Checks the condition expression.
	 *
	 * @param conditionNode
	 *            Bound node that represents condition expression.
	 * @param bindingContext
	 *            Binding context.
	 * @return <code>conditionNode</code> in case it is correct, and
	 *         {@link ErrorBoundNode} in case condition is wrong.
	 */
	public static IBoundNode checkConditionBoundNode(IBoundNode conditionNode,
			IBindingContext bindingContext) {
		if (conditionNode != null
				&& !isBooleanType(conditionNode.getType())) {
			if (conditionNode.getType() != NullOpenClass.the) {
				BindHelper.processError(CONDITION_TYPE_MESSAGE,
						conditionNode.getSyntaxNode(), bindingContext);
			}
			return new ErrorBoundNode(conditionNode.getSyntaxNode());
		} else {
			if (conditionNode != null) {
				checkForSameLeftAndRightExpression(conditionNode, bindingContext);
			}

			return conditionNode;
		}
	}

	private static void checkForSameLeftAndRightExpression(IBoundNode conditionNode, IBindingContext bindingContext) {
		IBoundNode[] children = conditionNode.getChildren();
		if (children.length == 2) {
			IBoundNode left = children[0];
			IBoundNode right = children[1];
			if (left instanceof FieldBoundNode && right instanceof FieldBoundNode
					&& ((FieldBoundNode) left).getBoundField() == ((FieldBoundNode) right).getBoundField()) {
				String type = conditionNode.getSyntaxNode().getType();

				if (EQUAL_OPERATORS.contains(type)) {
					BindHelper.processWarn("Condition is always true",
							conditionNode.getSyntaxNode(),
							bindingContext);
				} else if (NOT_EQUAL_OPERATORS.contains(type)) {
					BindHelper.processWarn("Condition is always false",
							conditionNode.getSyntaxNode(),
							bindingContext);
				}
			}
		}
	}

	public static void processWarn(String message, ISyntaxNode source,
			IBindingContext bindingContext) {
		if (bindingContext.isExecutionMode()) {
			OpenLMessagesUtils.addWarn(message);
		} else {
			OpenLMessagesUtils.addWarn(message, source);
		}
	}

	public static IBoundCode makeInvalidCode(IParsedCode parsedCode,
			ISyntaxNode syntaxNode, IBindingContext bindingContext) {

		ErrorBoundNode boundNode = new ErrorBoundNode(syntaxNode);

		return new BoundCode(parsedCode, boundNode, bindingContext.getErrors(),
				bindingContext.getLocalVarFrameSize());
	}

	public static IBoundCode makeInvalidCode(IParsedCode parsedCode,
			ISyntaxNode syntaxNode, SyntaxNodeException[] errors) {

		ErrorBoundNode boundNode = new ErrorBoundNode(syntaxNode);

		return new BoundCode(parsedCode, boundNode, errors, 0);
	}

	public static IBindingContext delegateContext(IBindingContext context,
			IBindingContextDelegator delegator) {

		if (delegator != null) {

			delegator.setTopDelegate(context);

			return delegator;
		}

		return context;
	}

	public static IBoundNode bindAsField(String fieldName, ISyntaxNode node,
			IBindingContext bindingContext, IBoundNode target) {
		try {
			IOpenField field = bindingContext.findFieldFor(target.getType(),
					fieldName, false);

			if (field == null) {
				String message = String.format("Field not found: '%s'",
						fieldName);
				BindHelper.processError(message, node, bindingContext, false);

				return new ErrorBoundNode(node);
			}

			if (target.isStaticTarget() != field.isStatic()) {

				if (field.isStatic()) {
					BindHelper.processWarn(
							"Access of a static field from non-static object",
							node, bindingContext);
				} else {
					BindHelper.processError(
							"Access non-static field from a static object",
							node, bindingContext);

					return new ErrorBoundNode(node);
				}
			}

			return new FieldBoundNode(node, field, target);

		} catch (Throwable t) {
			BindHelper.processError(node, t, bindingContext);

			return new ErrorBoundNode(node);
		}
	}

	/**
	 * Analyzes the binding context and returns the name for
	 * internal/temporary/service variable with the name: varNamePrefix + '$' +
	 * available_index.
	 */
	public static String getTemporaryVarName(IBindingContext bindingContext,
			String namespace, String varNamePrefix) {
		int index = 0;
		while (bindingContext.findVar(namespace, varNamePrefix + "$" + index,
				true) != null) {
			index++;
		}
		return varNamePrefix + "$" + index;
	}

	/**
     * Checks given type that it is boolean type.
     *
     * @param type {@link IOpenClass} instance
     * @return <code>true</code> if given type equals
     *         {@link JavaOpenClass#BOOLEAN} or
     *         JavaOpenClass.getOpenClass(Boolean.class); otherwise -
     *         <code>false</code>
     */
    private static boolean isBooleanType(IOpenClass type) {
        return type == null || JavaOpenClass.BOOLEAN == type || JavaOpenClass.getOpenClass(Boolean.class) == type;

    }
}
