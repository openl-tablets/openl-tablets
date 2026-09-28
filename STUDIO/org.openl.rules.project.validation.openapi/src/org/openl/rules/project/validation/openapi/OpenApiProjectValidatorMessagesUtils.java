package org.openl.rules.project.validation.openapi;

import java.util.Objects;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverterContextImpl;

import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.openapi.OpenAPIConfiguration;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.validation.ValidatedCompiledOpenClass;

final class OpenApiProjectValidatorMessagesUtils {

    private OpenApiProjectValidatorMessagesUtils() {
    }

    private static void addError(Context context, String summary) {
        var validatedCompiledOpenClass = context.getValidatedCompiledOpenClass();
        if (isNotExistingError(validatedCompiledOpenClass, summary)) {
            validatedCompiledOpenClass.addMessage(OpenLMessagesUtils.newErrorMessage(summary));
        }
    }

    private static boolean isNotExistingError(ValidatedCompiledOpenClass validatedCompiledOpenClass, String summary) {
        for (OpenLMessage openLMessage : validatedCompiledOpenClass.getAllMessages()) {
            if (openLMessage.isError() && Objects.equals(openLMessage.getSummary(), summary)) {
                return false;
            }
        }
        return true;
    }

    public static void addMethodError(Context context, String summary) {
        addMethodError(context, context.getOpenMethod(), summary);
    }

    private static void addMethodError(Context context, IOpenMethod method, String summary) {
        if (method instanceof OpenMethodDispatcher openMethodDispatcher) {
            for (IOpenMethod m : openMethodDispatcher.getCandidates()) {
                if (context.getField() != null && m instanceof Spreadsheet spreadsheet && m
                        .getType() instanceof CustomSpreadsheetResultOpenClass) {
                    addSpreadsheetFieldError(context, spreadsheet, summary);
                } else {
                    addMethodError(context, m, summary);
                }
            }
        } else {
            addTableError(context, method, summary);
        }
    }

    /**
     * Adds the error to the spreadsheet candidate that has a cell for the field in question. When the error is about
     * incompatible types, it is added only if the type of that cell is incompatible too.
     */
    private static void addSpreadsheetFieldError(Context context, Spreadsheet spreadsheet, String summary) {
        IOpenField openFieldInSpr = SpreadsheetMethodResolver.findSpreadsheetOpenField(spreadsheet,
                context.getField());
        if (openFieldInSpr != null) {
            if (context.getIsIncompatibleTypesPredicate() != null) {
                Class<?> instanceClass = resolveInstanceClass(openFieldInSpr);
                var openAPIContext = new ModelConverterContextImpl(
                        OpenAPIConfiguration.getConverters(context.getObjectMapper()));
                var actualSchema = openAPIContext.resolve(new AnnotatedType().type(instanceClass));
                if (context.getIsIncompatibleTypesPredicate().test(actualSchema, openFieldInSpr)) {
                    addMethodError(context, spreadsheet, summary);
                }
            } else {
                addMethodError(context, spreadsheet, summary);
            }
        }
    }

    private static Class<?> resolveInstanceClass(IOpenField openFieldInSpr) {
        Class<?> instanceClass;
        if (openFieldInSpr.getType() instanceof SpreadsheetResultOpenClass sprType
                && sprType.getModule() != null) {
            instanceClass = sprType.toCustomSpreadsheetResultOpenClass().getBeanClass();
        } else if (openFieldInSpr.getType() instanceof CustomSpreadsheetResultOpenClass csrType) {
            instanceClass = csrType.getBeanClass();
        } else {
            instanceClass = openFieldInSpr.getType().getInstanceClass();
        }
        if (instanceClass == null) {
            instanceClass = Object.class;
        }
        return instanceClass;
    }

    private static void addTableError(Context context, IOpenMethod method, String summary) {
        TableSyntaxNode tableSyntaxNode = extractTableSyntaxNode(method);
        if (tableSyntaxNode != null) {
            SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils.createError(summary,
                    tableSyntaxNode);
            if (isNotExistingError(context.getValidatedCompiledOpenClass(), summary)) {
                OpenLMessage openLMessage = OpenLMessagesUtils.newErrorMessage(syntaxNodeException);
                context.getValidatedCompiledOpenClass().addMessage(openLMessage);
            }
        } else {
            addError(context, summary);
        }
    }

    private static IOpenClass findOpenClass(Context context) {
        for (IOpenClass openClass : context.getOpenClass().getTypes()) {
            if (Objects.equals(context.getType().getInstanceClass(), openClass.getInstanceClass())) {
                return openClass;
            }
        }
        return context.getType();
    }

    public static void addTypeError(Context context, String summary) {
        IOpenClass type = findOpenClass(context);
        if (type instanceof DatatypeOpenClass datatypeOpenClass) {
            if (datatypeOpenClass.getTableSyntaxNode() != null) {
                SyntaxNodeException syntaxNodeException = SyntaxNodeExceptionUtils.createError(summary,
                        datatypeOpenClass.getTableSyntaxNode());
                if (isNotExistingError(context.getValidatedCompiledOpenClass(), summary)) {
                    OpenLMessage openLMessage = OpenLMessagesUtils.newErrorMessage(syntaxNodeException);
                    context.getValidatedCompiledOpenClass().addMessage(openLMessage);
                }
            }
        } else {
            var method = context.getSpreadsheetMethodResolver().resolve(type);
            if (method != null) {
                addMethodError(context, method, summary);
            } else {
                addError(context, summary);
            }
        }
    }

    private static TableSyntaxNode extractTableSyntaxNode(IOpenMethod method) {
        if (method instanceof ExecutableRulesMethod executableRulesMethod) {
            return executableRulesMethod.getSyntaxNode();
        }
        return null;
    }

    public static void addMethodWarning(Context context, String summary) {

        var compiledOpenClass = context.getValidatedCompiledOpenClass();
        compiledOpenClass.getAllMessages()
                .stream()
                .filter(x -> x.getSeverity().equals(Severity.WARN))
                .filter(x -> Objects.equals(x.getSummary(), summary))
                .findFirst()
                .ifPresentOrElse(x -> {
                }, () -> addWarn(summary, compiledOpenClass, context));
    }

    private static void addWarn(String summary, ValidatedCompiledOpenClass compiledOpenClass, Context context) {
        var tsn = extractTableSyntaxNode(context.getOpenMethod());
        var message = tsn == null ? OpenLMessagesUtils.newWarnMessage(summary)
                : OpenLMessagesUtils.newWarnMessage(summary, tsn);
        compiledOpenClass.addMessage(message);
    }
}
