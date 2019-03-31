package org.openl.codegen.tools.type;

import org.openl.rules.table.constraints.Constraint;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.constraints.DataEnumConstraint;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.expressions.match.MatchingExpression;

public class TablePropertyDefinitionWrapper {

    private TablePropertyDefinition tablePropertyDefinition;
    private String operationName;
    private String contextVar;
    private String propertyVar;

    public TablePropertyDefinitionWrapper(TablePropertyDefinition tablePropertyDefinition) {
        this.tablePropertyDefinition = tablePropertyDefinition;

        init();
    }

    private void init() {

        MatchingExpression expression = tablePropertyDefinition.getExpression();

        if (expression != null) {
            operationName = expression.getMatchExpression().getOperationName();
            propertyVar = tablePropertyDefinition.getName();
            contextVar = expression.getMatchExpression().getContextAttribute();
        }
    }

    public TablePropertyDefinition getDefinition() {
        return tablePropertyDefinition;
    }

    public String getOperation() {
        return operationName;
    }

    public String getContextVar() {
        return contextVar;
    }

    public String getPropertyVar() {
        return propertyVar;
    }

    public boolean isEnum() {
        return org.openl.rules.enumeration.Enum.class
            .equals(tablePropertyDefinition.getType().getInstanceClass()) || org.openl.rules.enumeration.Enum[].class
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