package org.open.rules.project.validation.openapi;

import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

final class OpenApiProjectValidatorMessagesUtils {

    private OpenApiProjectValidatorMessagesUtils() {
    }

    public static void addError(Context context, String summary) {
        context.getValidatedCompiledOpenClass().addValidationMessage(OpenLMessagesUtils.newErrorMessage(summary));
    }

    public static void addWarning(Context context, String summary) {
        context.getValidatedCompiledOpenClass().addValidationMessage(OpenLMessagesUtils.newWarnMessage(summary));
    }

    public static void addMethodError(Context context, String summary) {
        TableSyntaxNode tableSyntaxNode = extractTableSyntaxNode(context.getOpenMethod());
        if (tableSyntaxNode != null) {
            SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils.createError(summary, tableSyntaxNode);
            OpenLMessage openLMessage = OpenLMessagesUtils.newErrorMessage(syntaxNodeException);
            context.getValidatedCompiledOpenClass().addValidationMessage(openLMessage);
            tableSyntaxNode.addError(syntaxNodeException);
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
        } else {
            IOpenMethod method = context.getSpreadsheetMethodResolver().resolve(context.getType());
            if (method != null) {
                TableSyntaxNode tableSyntaxNode = extractTableSyntaxNode(context.getOpenMethod());
                if (tableSyntaxNode != null) {
                    SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils
                        .createError(summary, tableSyntaxNode);
                    OpenLMessage openLMessage = OpenLMessagesUtils.newErrorMessage(syntaxNodeException);
                    context.getValidatedCompiledOpenClass().addValidationMessage(openLMessage);
                    tableSyntaxNode.addError(syntaxNodeException);
                }
            }
        }
        addError(context, summary);
    }

    private static TableSyntaxNode extractTableSyntaxNode(IOpenMethod method) {
        if (method instanceof ExecutableRulesMethod) {
            ExecutableRulesMethod executableRulesMethod = (ExecutableRulesMethod) method;
            return executableRulesMethod.getSyntaxNode();
        }
        return null;
    }

    public static void addMethodWarning(Context context, String summary) {
        TableSyntaxNode tableSyntaxNode = extractTableSyntaxNode(context.getOpenMethod());
        if (tableSyntaxNode != null) {
            OpenLMessage openLMessage = OpenLMessagesUtils.newWarnMessage(summary, tableSyntaxNode);
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
