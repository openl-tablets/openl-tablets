package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.binding.error.BoundErrorUtils;
import org.openl.binding.error.IBoundError;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.OpenLWarnMessage;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;

public abstract class BindHelper {

    public static void processError(ISyntaxNode syntaxNode, Throwable throwable, IBindingContext bindingContext) {

        IBoundError error = BoundErrorUtils.createError(throwable, syntaxNode);
        processError(error, bindingContext);
    }

    public static void processError(String message,
                                    ISyntaxNode syntaxNode,
                                    Throwable throwable,
                                    IBindingContext bindingContext) {

        IBoundError error = BoundErrorUtils.createError(message, throwable, syntaxNode);
        processError(error, bindingContext);
    }

    public static void processError(IBoundError error, IBindingContext bindingContext) {

        bindingContext.addError(error);
        processError(error);
    }

    public static void processError(String message, ISyntaxNode syntaxNode, IBindingContext bindingContext) {

        IBoundError error = BoundErrorUtils.createError(message, syntaxNode);
        processError(error, bindingContext);
    }

    public static void processError(String message, ISyntaxNode syntaxNode, Throwable throwable) {

        IBoundError error = BoundErrorUtils.createError(message, throwable, syntaxNode);
        processError(error);
    }

    public static void processError(IBoundError error) {
        OpenLMessagesUtils.addError(error);
    }

    public static void processWarn(String message, ISyntaxNode source) {

        OpenLWarnMessage warn = new OpenLWarnMessage(message, source);
        OpenLMessagesUtils.addMessage(warn);
    }

    public static IBoundCode makeInvalidCode(IParsedCode parsedCode,
                                             ISyntaxNode syntaxNode,
                                             IBindingContext bindingContext) {

        ErrorBoundNode boundNode = new ErrorBoundNode(syntaxNode);

        return new BoundCode(parsedCode, boundNode, bindingContext.getErrors(), bindingContext.getLocalVarFrameSize());
    }

    public static IBoundCode makeInvalidCode(IParsedCode parsedCode, ISyntaxNode syntaxNode, IBoundError[] errors) {

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
}
