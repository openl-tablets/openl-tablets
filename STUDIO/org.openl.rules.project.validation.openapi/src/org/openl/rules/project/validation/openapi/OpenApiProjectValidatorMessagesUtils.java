package org.openl.rules.project.validation.openapi;

import java.util.Objects;

import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.project.validation.base.ValidatedCompiledOpenClass;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;

import io.swagger.v3.oas.models.media.Schema;

final class OpenApiProjectValidatorMessagesUtils {

    private OpenApiProjectValidatorMessagesUtils() {
    }

    public static void addError(Context context, String summary) {
        ValidatedCompiledOpenClass validatedCompiledOpenClass = context.getValidatedCompiledOpenClass();
        if (isNotExistingError(validatedCompiledOpenClass, summary)) {
            validatedCompiledOpenClass.addValidationMessage(OpenLMessagesUtils.newErrorMessage(summary));
        }
    }

    private static boolean isNotExistingError(ValidatedCompiledOpenClass validatedCompiledOpenClass, String summary) {
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
        addMethodError(context, context.getOpenMethod(), summary);
    }

    @SuppressWarnings("rawtypes")
    public static void addMethodError(Context context, IOpenMethod method, String summary) {
        if (method instanceof OpenMethodDispatcher) {
            OpenMethodDispatcher openMethodDispatcher = (OpenMethodDispatcher) method;
            for (IOpenMethod m : openMethodDispatcher.getCandidates()) {
                if (context.getField() != null && m instanceof Spreadsheet && m
                    .getType() instanceof CustomSpreadsheetResultOpenClass) {
                    IOpenField openFieldInSpr = SpreadsheetMethodResolver.findSpreadsheetOpenField((Spreadsheet) m,
                        context.getField());
                    if (openFieldInSpr != null) {
                        if (context.getIsIncompatibleTypesPredicate() != null) {
                            Schema actualSchema = SchemaResolver.resolve(context,
                                openFieldInSpr.getType().getInstanceClass());
                            if (context.getIsIncompatibleTypesPredicate().test(actualSchema, openFieldInSpr)) {
                                addMethodError(context, m, summary);
                            }
                        } else {
                            addMethodError(context, m, summary);
                        }
                    }
                } else {
                    addMethodError(context, m, summary);
                }
            }
        } else {
            TableSyntaxNode tableSyntaxNode = extractTableSyntaxNode(method);
            if (tableSyntaxNode != null) {
                SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils.createError(summary,
                    tableSyntaxNode);
                if (isNotExistingError(context.getValidatedCompiledOpenClass(), summary)) {
                    OpenLMessage openLMessage = OpenLMessagesUtils.newErrorMessage(syntaxNodeException);
                    context.getValidatedCompiledOpenClass().addValidationMessage(openLMessage);
                }
            } else {
                addError(context, summary);
            }
        }
    }

    public static void addTypeError(Context context, String summary) {
        if (context.getType() instanceof DatatypeOpenClass) {
            DatatypeOpenClass datatypeOpenClass = (DatatypeOpenClass) context.getType();
            if (datatypeOpenClass.getTableSyntaxNode() != null) {
                SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils.createError(summary,
                    datatypeOpenClass.getTableSyntaxNode());
                if (isNotExistingError(context.getValidatedCompiledOpenClass(), summary)) {
                    OpenLMessage openLMessage = OpenLMessagesUtils.newErrorMessage(syntaxNodeException);
                    context.getValidatedCompiledOpenClass().addValidationMessage(openLMessage);
                }
            }
        } else {
            IOpenMethod method = context.getSpreadsheetMethodResolver().resolve(context.getType());
            if (method != null) {
                addMethodError(context, method, summary);
            } else {
                addError(context, summary);
            }
        }
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
        if (tableSyntaxNode != null && isNotExistingError(context.getValidatedCompiledOpenClass(), summary)) {
            OpenLMessage openLMessage = OpenLMessagesUtils.newWarnMessage(summary, tableSyntaxNode);
            context.getValidatedCompiledOpenClass().addValidationMessage(openLMessage);
        } else {
            addWarning(context, summary);
        }
    }
}
