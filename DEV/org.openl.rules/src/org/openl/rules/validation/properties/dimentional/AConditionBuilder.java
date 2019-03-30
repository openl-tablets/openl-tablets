package org.openl.rules.validation.properties.dimentional;

import org.openl.rules.table.IWritableGrid;

public abstract class AConditionBuilder implements IDecisionTableColumnBuilder {

    private IDecisionTableColumn condition;

    private int conditionNumber;

    AConditionBuilder(IDecisionTableColumn condition, int conditionNumber) {
        this.condition = condition;
        this.conditionNumber = conditionNumber;
    }

    @Override
    public final int build(IWritableGrid gridModel, int numberOfRules, int columnStartIndex, int rowStartIndex) {
        writeColumnType(gridModel, columnStartIndex, rowStartIndex);
        writeCodeExpression(gridModel, columnStartIndex, rowStartIndex);
        writeParameterDeclaration(gridModel, columnStartIndex, rowStartIndex);
        writeTitle(gridModel, columnStartIndex, rowStartIndex);
        writeRuleValue(gridModel, numberOfRules, columnStartIndex, rowStartIndex);
        return condition.getNumberOfLocalParameters();
    }

    public IDecisionTableColumn getCondition() {
        return condition;
    }

    int getConditionNumber() {
        return conditionNumber;
    }

    protected abstract void writeColumnType(IWritableGrid gridModel, int columnStartIndex, int rowStartIndex);

    protected abstract void writeCodeExpression(IWritableGrid gridModel, int columnStartIndex, int rowStartIndex);

    protected abstract void writeParameterDeclaration(IWritableGrid gridModel, int columnStartIndex, int rowStartIndex);

    protected abstract void writeTitle(IWritableGrid gridModel, int columnStartIndex, int rowStartIndex);

    protected abstract void writeRuleValue(IWritableGrid gridModel,
            int numberOfRules,
            int columnStartIndex,
            int rowStartIndex);
}
