package org.openl.rules.dt.builder;

import org.openl.rules.table.IWritableGrid;
import org.openl.rules.validation.properties.dimentional.DispatcherTableColumnMaker;
import org.openl.rules.validation.properties.dimentional.IDecisionTableColumn;

import java.util.ArrayList;
import java.util.List;

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
     * @param conditions
     */
    private void initConditionBuilders(List<IDecisionTableColumn> conditions) { 
        int conditionNumber = 0;
        conditionBuilders = new ArrayList<IDecisionTableColumnBuilder>();
        for (int i = 0; i < conditions.size(); i++) {
            IDecisionTableColumn condition = conditions.get(i);
            if (condition.getNumberOfLocalParameters() > 0) {
                // Process only conditions that have local parameters, and skip others
                //
                conditionNumber++;
                conditionBuilders.add(DispatcherTableColumnMaker.getConditionBuilder(condition, conditionNumber));
            }
        }
    }

}
