package org.open.rules.project.validation.openapi;

import java.util.Objects;

import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.project.validation.base.ValidatedCompiledOpenClass;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

final class OpenApiProjectValidatorMessagesUtils {

    private OpenApiProjectValidatorMessagesUtils() {
    }

    public static void addError(Context context, String summary) {
        ValidatedCompiledOpenClass validatedCompiledOpenClass = context.getValidatedCompiledOpenClass();
        if (isNotExistingError(summary, validatedCompiledOpenClass)) {
            validatedCompiledOpenClass.addValidationMessage(OpenLMessagesUtils.newErrorMessage(summary));
        }
    }

    private static boolean isNotExistingError(String summary, ValidatedCompiledOpenClass validatedCompiledOpenClass) {
        for (OpenLMessage openLMessage : validatedCompiledOpenClass.getMessages()) {
            if (openLMessage.isError() && Objects.equals(openLMessage.getSummary(), summary)) {
                return false;
            }
        }
        return true;
    }

    public static void addWarning(Context context, String summary) {
        ValidatedCompiledOpenClass validatedCompiledOpenClass = context.getValidatedCompiledOpenClass();
        if (isNotExistingWarning(summary, validatedCompiledOpenClass)) {
            validatedCompiledOpenClass.addValidationMessage(OpenLMessagesUtils.newWarnMessage(summary));
        }
    }

    private static boolean isNotExistingWarning(String summary, ValidatedCompiledOpenClass validatedCompiledOpenClass) {
        for (OpenLMessage openLMessage : validatedCompiledOpenClass.getMessages()) {
            if (openLMessage.isWarn() && Objects.equals(openLMessage.getSummary(), summary)) {
                return false;
            }
        }
        return true;
    }

    public static void addMethodError(Context context, String summary) {
        TableSyntaxNode tableSyntaxNode = extractTableSyntaxNode(context.getOpenMethod());
        if (tableSyntaxNode != null) {
            SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils.createError(summary, tableSyntaxNode);
            OpenLMessage openLMessage = OpenLMessagesUtils.newErrorMessage(syntaxNodeException);
            context.getValidatedCompiledOpenClass().addValidationMessage(openLMessage);
            if (isNotExistingError(tableSyntaxNode, summary)) {
                tableSyntaxNode.addError(syntaxNodeException);
            }
        } else {
            addError(context, summary);
        }
    }

    private static boolean isNotExistingError(TableSyntaxNode tableSyntaxNode, String summary) {
        for (SyntaxNodeException sne : tableSyntaxNode.getErrors()) {
            if (Objects.equals(sne.getMessage(), summary)) {
                return false;
            }
        }
        return true;
    }

    public static void addTypeError(Context context, String summary) {
        if (context.getType() instanceof DatatypeOpenClass) {
            DatatypeOpenClass datatypeOpenClass = (DatatypeOpenClass) context.getType();
            if (datatypeOpenClass.getTableSyntaxNode() != null) {
                if (isNotExistingError(datatypeOpenClass.getTableSyntaxNode(), summary)) {
                    SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils.createError(summary,
                        datatypeOpenClass.getTableSyntaxNode());
                    OpenLMessage openLMessage = OpenLMessagesUtils.newErrorMessage(syntaxNodeException);
                    context.getValidatedCompiledOpenClass().addValidationMessage(openLMessage);
                    datatypeOpenClass.getTableSyntaxNode().addError(syntaxNodeException);
                }
                return;
            }
        } else {
            IOpenMethod method = context.getSpreadsheetMethodResolver().resolve(context.getType());
            if (method != null) {
                TableSyntaxNode tableSyntaxNode = extractTableSyntaxNode(context.getOpenMethod());
                if (tableSyntaxNode != null) {
                    if (isNotExistingError(tableSyntaxNode, summary)) {
                        SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils.createError(summary,
                            tableSyntaxNode);
                        OpenLMessage openLMessage = OpenLMessagesUtils.newErrorMessage(syntaxNodeException);
                        context.getValidatedCompiledOpenClass().addValidationMessage(openLMessage);
                        tableSyntaxNode.addError(syntaxNodeException);
                    }
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
        if (tableSyntaxNode != null && isNotExistingError(tableSyntaxNode, summary)) {
            OpenLMessage openLMessage = OpenLMessagesUtils.newWarnMessage(summary, tableSyntaxNode);
            context.getValidatedCompiledOpenClass().addValidationMessage(openLMessage);
        } else {
            addWarning(context, summary);
        }
    }

    public static void addTypeWarning(Context context, IOpenClass openClass, String summary) {
        if (openClass instanceof DatatypeOpenClass) {
            DatatypeOpenClass datatypeOpenClass = (DatatypeOpenClass) openClass;
            if (datatypeOpenClass.getTableSyntaxNode() != null && isNotExistingError(
                datatypeOpenClass.getTableSyntaxNode(),
                summary)) {
                OpenLMessage openLMessage = OpenLMessagesUtils.newWarnMessage(summary,
                    datatypeOpenClass.getTableSyntaxNode());
                context.getValidatedCompiledOpenClass().addValidationMessage(openLMessage);
            }
            return;
        }
        addWarning(context, summary);
    }
}
