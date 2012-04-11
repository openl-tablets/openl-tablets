package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.OpenLWarnMessage;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;

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

    public static void processError(String message, ISyntaxNode syntaxNode, IBindingContext bindingContext) {
        processError(message, syntaxNode, bindingContext, true);
    }

    public static void processError(String message, ISyntaxNode syntaxNode, IBindingContext bindingContext,
            boolean storeGlobal) {
        SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, syntaxNode);
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
}
