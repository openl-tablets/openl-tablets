package org.openl.codegen.tools;

import org.openl.rules.table.properties.TablePropertyDefinition;

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

        int openBracketIndex = expression.indexOf("(");
        int closeBracketIndex = expression.indexOf(")");

        operation = expression.substring(0, openBracketIndex).toUpperCase();
        propertyVar = tablePropertyDefinition.getName();
        contextVar = expression.substring(openBracketIndex + 1, closeBracketIndex);
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
}