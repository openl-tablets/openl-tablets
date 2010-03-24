package org.openl.codegen.tools.type;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.table.constraints.Constraint;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.constraints.DataEnumConstraint;
import org.openl.rules.table.properties.def.TablePropertyDefinition;

public class TablePropertyDefinitionWrapper {

    private TablePropertyDefinition tablePropertyDefinition;
    private String operation;
    private String contextVar;
    private String propertyVar;

    public TablePropertyDefinitionWrapper(TablePropertyDefinition tablePropertyDefinition) {
        this.tablePropertyDefinition = tablePropertyDefinition;

        init();
    }

    private void init() {

        String expression = tablePropertyDefinition.getExpression();

        if (!StringUtils.isEmpty(expression)) {
            int openBracketIndex = expression.indexOf("(");
            int closeBracketIndex = expression.indexOf(")");

            operation = expression.substring(0, openBracketIndex).toUpperCase();
            propertyVar = tablePropertyDefinition.getName();
            contextVar = expression.substring(openBracketIndex + 1, closeBracketIndex);
        }
    }

    public TablePropertyDefinition getDefinition() {
        return tablePropertyDefinition;
    }

    public String getOperation() {
        return operation;
    }

    public String getContextVar() {
        return contextVar;
    }

    public String getPropertyVar() {
        return propertyVar;
    }

    public boolean isEnum() {
        return org.openl.rules.enumeration.Enum.class.equals(tablePropertyDefinition.getType().getInstanceClass())
                || org.openl.rules.enumeration.Enum[].class
                        .equals(tablePropertyDefinition.getType().getInstanceClass());
    }

    public String getEnumName() {

        Constraints constraints = tablePropertyDefinition.getConstraints();
        Constraint dataEnumConstraint = null;

        for (int i = 0; i < constraints.size(); i++) {

            Constraint constraint = constraints.get(i);

            if (constraint instanceof DataEnumConstraint) {

                if (dataEnumConstraint != null) {
                    throw new RuntimeException("Ambiguous enumearation definitions");
                } else {
                    dataEnumConstraint = constraint;
                }
            }
        }

        if (dataEnumConstraint == null) {
            throw new RuntimeException("Undefined enumeration constraint");
        }

        Object[] params = dataEnumConstraint.getParams();

        return (String) params[0];
    }
}