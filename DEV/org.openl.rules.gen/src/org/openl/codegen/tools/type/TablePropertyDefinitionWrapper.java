package org.openl.codegen.tools.type;

import org.openl.rules.table.properties.def.TablePropertyDefinition;

public class TablePropertyDefinitionWrapper {

    private final TablePropertyDefinition tablePropertyDefinition;
    private String operationName;
    private String contextVar;
    private String propertyVar;

    public TablePropertyDefinitionWrapper(TablePropertyDefinition tablePropertyDefinition) {
        this.tablePropertyDefinition = tablePropertyDefinition;

        init();
    }

    private void init() {

        var expression = tablePropertyDefinition.getExpression();

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
}
