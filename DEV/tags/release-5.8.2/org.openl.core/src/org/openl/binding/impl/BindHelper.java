package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundNode;
import org.openl.message.OpenLMessagesUtils;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenField;
import org.openl.types.java.OpenClassHelper;

public class BindHelper {
    
    private BindHelper() {
    }

    public static void processError(ISyntaxNode syntaxNode, Throwable throwable, IBindingContext bindingContext) {

        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(throwable, syntaxNode);
        processError(error, bindingContext);
    }

    public static void processError(String message,
                                    ISyntaxNode syntaxNode,
                                    Throwable throwable,
                                    IBindingContext bindingContext) {

        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, throwable, syntaxNode);
        processError(error, bindingContext);
    }

    public static void processError(SyntaxNodeException error, IBindingContext bindingContext) {

        bindingContext.addError(error);
        processError(error);
    }
    
    public static void processError(Throwable error, ISyntaxNode syntaxNode, IBindingContext bindingContext) {
        processError(error, syntaxNode, bindingContext, true);
    }
    
    public static void processError(Throwable error, ISyntaxNode syntaxNode, IBindingContext bindingContext, boolean storeGlobal) {
        SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils.createError(error, syntaxNode);
        processSyntaxNodeException(syntaxNodeException, storeGlobal, bindingContext);
    }

    public static void processError(String message, ISyntaxNode syntaxNode, IBindingContext bindingContext) {
        processError(message, syntaxNode, bindingContext, true);
    }

    public static void processError(String message, ISyntaxNode syntaxNode, IBindingContext bindingContext,
            boolean storeGlobal) {
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, syntaxNode);
        processSyntaxNodeException(error, storeGlobal, bindingContext);
    }

    private static void processSyntaxNodeException(SyntaxNodeException error, boolean storeGlobal,
            IBindingContext bindingContext) {
        bindingContext.addError(error);
        if (storeGlobal) {
            processError(error);
        }
    }

    public static void processError(String message, ISyntaxNode syntaxNode, Throwable throwable) {

        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, throwable, syntaxNode);
        processError(error);
    }
    
    public static void processError(String message, ISyntaxNode syntaxNode) {

        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, syntaxNode);
        processError(error);
    }

    public static void processError(SyntaxNodeException error) {
        OpenLMessagesUtils.addError(error);
    }
    

    public static final String CONDITION_TYPE_MESSAGE = "Condition must have boolean type";
    /**
     * Checks the condition expression.
     * 
     * @param conditionNode Bound node that represents condition expression.
     * @param bindingContext Binding context.
     * @return <code>conditionNode</code> in case it is correct, and {@link ErrorBoundNode} in case condition is wrong.
     */
    public static IBoundNode checkConditionBoundNode(IBoundNode conditionNode,
            IBindingContext bindingContext) {
        if (conditionNode != null && !OpenClassHelper.isBooleanType(conditionNode.getType())) {
            BindHelper.processError(CONDITION_TYPE_MESSAGE, conditionNode.getSyntaxNode(), bindingContext);
            return new ErrorBoundNode(conditionNode.getSyntaxNode());
        }else{
            return conditionNode;
        }
    }

    public static void processWarn(String message, ISyntaxNode source, IBindingContext bindingContext) {
        if (bindingContext.isExecutionMode()) {
            OpenLMessagesUtils.addWarn(message);
        } else {
            OpenLMessagesUtils.addWarn(message, source);
        }
    }

    public static IBoundCode makeInvalidCode(IParsedCode parsedCode,
                                             ISyntaxNode syntaxNode,
                                             IBindingContext bindingContext) {

        ErrorBoundNode boundNode = new ErrorBoundNode(syntaxNode);

        return new BoundCode(parsedCode, boundNode, bindingContext.getErrors(), bindingContext.getLocalVarFrameSize());
    }

    public static IBoundCode makeInvalidCode(IParsedCode parsedCode, ISyntaxNode syntaxNode, SyntaxNodeException[] errors) {

        ErrorBoundNode boundNode = new ErrorBoundNode(syntaxNode);

        return new BoundCode(parsedCode, boundNode, errors, 0);
    }

    public static IBindingContext delegateContext(IBindingContext context, IBindingContextDelegator delegator) {

        if (delegator != null) {

            delegator.setTopDelegate(context);

            return delegator;
        }

        return context;
    }
    
    public static IBoundNode bindAsField(String fieldName, ISyntaxNode node, IBindingContext bindingContext, 
            IBoundNode target) {
        try {
            IOpenField field = bindingContext.findFieldFor(target.getType(), fieldName, false);

            if (field == null) {
                String message = String.format("Field not found: '%s'", fieldName);
                BindHelper.processError(message, node, bindingContext, false);

                return new ErrorBoundNode(node);
            }

            if (target.isStaticTarget() != field.isStatic()) {

                if (field.isStatic()) {
                    BindHelper.processWarn("Access of a static field from non-static object", node, bindingContext);
                } else {
                    BindHelper.processError("Access non-static field from a static object", node, bindingContext);

                    return new ErrorBoundNode(node);
                }
            }

            return new FieldBoundNode(node, field, target);

        } catch (Throwable t) {
            BindHelper.processError(node, t, bindingContext);

            return new ErrorBoundNode(node);
        }
    }

    
}
