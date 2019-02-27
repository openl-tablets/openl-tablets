package org.openl.rules.validation.properties.dimentional;

import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;

/**
 * This class is used to build dispatcher table by dimensional properties for the group of overloaded tables. In this
 * table in return column there are calls for original table from the group for every rule.
 * 
 * @author DLiauchuk
 *
 */
public class DispatcherTableReturnColumn {

    private static final String RESULT_VAR = "result";
    /**
     * Return type of the member of overloaded tables group.
     */
    private IOpenClass originalReturnType;
    /**
     * Name of method in overloaded tables group.
     */
    private String methodName;
    /**
     * Signature of the member of overloaded tables group.
     */
    private IMethodSignature originalSignature;

    DispatcherTableReturnColumn(IOpenClass originalReturnType, String methodName, IMethodSignature originalSignature) {
        this.originalReturnType = originalReturnType;
        this.methodName = methodName;
        this.originalSignature = originalSignature;
    }

    public String getParameterDeclaration() {
        return String.format("%s %s", getReturnType().getDisplayName(0), getCodeExpression());
    }

    public String getCodeExpression() {
        return RESULT_VAR;
    }

    public String getTitle() {
        return getCodeExpression().toUpperCase();
    }

    public String getRuleValue(int ruleIndex, int elementNum) {
        final StringBuilder builder = new StringBuilder(128);
        builder.append('=')
            .append(methodName)
            .append(TableSyntaxNodeDispatcherBuilder.AUXILIARY_METHOD_DELIMETER)
            .append(ruleIndex)
            .append('(');

        boolean prependComma = false;
        final int numberOfParameters = originalSignature.getNumberOfParameters();
        for (int i = 0; i < numberOfParameters; i++) {
            final String parameterName = originalSignature.getParameterName(i);
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

    IMethodSignature getOriginalSignature() {
        return originalSignature;
    }

    public String getRuleValue(int ruleIndex) {
        return getRuleValue(ruleIndex, 0);
    }
}
