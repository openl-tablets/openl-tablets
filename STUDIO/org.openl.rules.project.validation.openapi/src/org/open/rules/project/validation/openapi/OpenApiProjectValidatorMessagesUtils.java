package org.open.rules.project.validation.openapi;

import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;

final class OpenApiProjectValidatorMessagesUtils {
    public static void addError(Context context, String summary) {
        context.getValidatedCompiledOpenClass().addValidationMessage(OpenLMessagesUtils.newErrorMessage(summary));
    }

    public static void addWarning(Context context, String summary) {
        context.getValidatedCompiledOpenClass().addValidationMessage(OpenLMessagesUtils.newWarnMessage(summary));
    }

    public static void addMethodError(Context context, String summary) {
        if (context.getTableSyntaxNode() != null) {
            SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils.createError(summary,
                context.getTableSyntaxNode());
            OpenLMessage openLMessage = OpenLMessagesUtils.newErrorMessage(syntaxNodeException);
            context.getValidatedCompiledOpenClass().addValidationMessage(openLMessage);
            context.getTableSyntaxNode().addError(syntaxNodeException);
        } else {
            addError(context, summary);
        }
    }

    public static void addTypeError(Context context, String summary) {
        if (context.getType() instanceof DatatypeOpenClass) {
            DatatypeOpenClass datatypeOpenClass = (DatatypeOpenClass) context.getType();
            if (datatypeOpenClass.getTableSyntaxNode() != null) {
                SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils.createError(summary,
                    datatypeOpenClass.getTableSyntaxNode());
                OpenLMessage openLMessage = OpenLMessagesUtils.newErrorMessage(syntaxNodeException);
                context.getValidatedCompiledOpenClass().addValidationMessage(openLMessage);
                datatypeOpenClass.getTableSyntaxNode().addError(syntaxNodeException);
                return;
            }
        }
        addError(context, summary);
    }

    public static void addMethodWarning(Context context, String summary) {
        if (context.getTableSyntaxNode() != null) {
            OpenLMessage openLMessage = OpenLMessagesUtils.newWarnMessage(summary, context.getTableSyntaxNode());
            context.getValidatedCompiledOpenClass().addValidationMessage(openLMessage);
        } else {
            addWarning(context, summary);
        }
    }

    public static void addTypeWarning(Context context, IOpenClass openClass, String summary) {
        if (openClass instanceof DatatypeOpenClass) {
            DatatypeOpenClass datatypeOpenClass = (DatatypeOpenClass) openClass;
            if (datatypeOpenClass.getTableSyntaxNode() != null) {
                OpenLMessage openLMessage = OpenLMessagesUtils.newWarnMessage(summary,
                    datatypeOpenClass.getTableSyntaxNode());
                context.getValidatedCompiledOpenClass().addValidationMessage(openLMessage);
                return;
            }
        }
        context.getValidatedCompiledOpenClass().addValidationMessage(OpenLMessagesUtils.newWarnMessage(summary));
    }
}
