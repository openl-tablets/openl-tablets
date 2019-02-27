package org.openl.rules.dt.builder;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.IWritableGrid;
import org.openl.rules.validation.properties.dimentional.IDecisionTableColumn;

/**
 * Aggregate builder to build the number of conditions to the source sheet.
 * 
 * @author DLiauchuk
 *
 */
public class ConditionsBuilder implements IDecisionTableColumnBuilder {

    private List<IDecisionTableColumnBuilder> conditionBuilders;

    public ConditionsBuilder(List<IDecisionTableColumn> conditions) {
        initConditionBuilders(conditions);
    }

    /**
     * Build the conditions to the sheet with the given number of rules and start writing from the given column index.
     * 
     * @param numberOfRules number of rules that will be written
     * @param columnStartIndex the index of the column on the sheet to start writing
     * @param sheet the place for writing conditions
     * 
     * @return the index of the column that is next to the written conditions and is free for further writing.
     */
    public int build(IWritableGrid sheet, int numberOfRules, int columnStartIndex, int rowStartIndex) {
        int nextColumn = columnStartIndex;

        for (IDecisionTableColumnBuilder conditionBuilder : conditionBuilders) {
            int columnUsedForCondition = conditionBuilder.build(sheet, numberOfRules, nextColumn, rowStartIndex);
            nextColumn += columnUsedForCondition;
        }
        return nextColumn;
    }

    /**
     * Wrap each condition with the appropriate builder.
     * 
     * @param conditions
     */
    private void initConditionBuilders(List<IDecisionTableColumn> conditions) {
        int conditionsNumber = 0;
        conditionBuilders = new ArrayList<IDecisionTableColumnBuilder>();
        for (IDecisionTableColumn condition : conditions) {
            if (condition.getNumberOfLocalParameters() > 0) {
                // process only conditions that have local parameters, other ones skip
                //
                conditionsNumber++;
                if (condition.getNumberOfLocalParameters() > 1) {
                    conditionBuilders.add(new ArrayConditionBuilder(condition, conditionsNumber));
                } else {
                    conditionBuilders.add(new SimpleConditionBuilder(condition, conditionsNumber));
                }
            }

        }
    }

}
