package org.openl.rules.validation.properties.dimentional;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;

/**
 * This class is used to build dispatcher table by dimensional properties for the group of overloaded tables. In this
 * table in return column there are calls for original table from the group for every rule.
 *
 * @author DLiauchuk
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class DispatcherTableReturnColumn {

    private static final String RESULT_VAR = "result";
    /**
     * Return type of the member of overloaded tables group.
     */
    private final IOpenClass originalReturnType;
    /**
     * Name of method in overloaded tables group.
     */
    private final String methodName;
    /**
     * Signature of the member of overloaded tables group.
     */
    @Getter(AccessLevel.PACKAGE)
    private final IMethodSignature originalSignature;

    public String getParameterDeclaration() {
        return "%s %s".formatted(getReturnType().getDisplayName(0), getCodeExpression());
    }

    public String getCodeExpression() {
        return RESULT_VAR;
    }

    public String getTitle() {
        return getCodeExpression().toUpperCase();
    }

    public String getRuleValue(int ruleIndex, int elementNum) {
        final var builder = new StringBuilder(128);
        builder.append('=')
                .append(methodName)
                .append(TableSyntaxNodeDispatcherBuilder.AUXILIARY_METHOD_DELIMETER)
                .append(ruleIndex)
                .append('(');

        var prependComma = false;
        final var numberOfParameters = originalSignature.getNumberOfParameters();
        for (var i = 0; i < numberOfParameters; i++) {
            final var parameterName = originalSignature.getParameterName(i);
            final String parameter = TableSyntaxNodeDispatcherBuilder
                    .getDispatcherParameterNameForOriginalParameter(parameterName);
            if (prependComma) {
                builder.append(',');
            }
            builder.append(parameter);
            prependComma = true;
        }

        builder.append(')');
        return builder.toString();
    }

    public IOpenClass getReturnType() {
        return originalReturnType;
    }

    public String getRuleValue(int ruleIndex) {
        return getRuleValue(ruleIndex, 0);
    }
}
