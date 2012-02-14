package org.openl.rules.dt.builder;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.Point;

/**
 * Creates the memory representation of DT table by POI.
 * 
 * @author DLiauchuk
 *
 */
public class DecisionTableBuilder {
    
    /** number 5 - is a number of first development rows in table.*/
    public static final int DECISION_TABLE_HEADER_ROWS_NUMBER = 5;  
    public static final int CONDITION_TITLE_ROW_INDEX = 4;
    public static final int PARAMETER_DECLARATION_ROW_INDEX = 3;
    public static final int CODE_EXPRESSION_ROW_INDEX = 2;
    
    /** condition name always is the next row after header row.*/
    public static final int COLUMN_TYPE_ROW_INDEX = 1; 
    
    private TableHeaderBuilder headerBuilder;
    private IDecisionTableColumnBuilder conditionsBuilder;
    private IDecisionTableColumnBuilder returnBuilder;
    private Point startPoint;
    
    public DecisionTableBuilder(Point startPoint) {        
        this.startPoint = startPoint;
    }  
    
    public Point getStartPoint() {
        return startPoint;
    }
    
    public void setStartPoint(Point startPoint) {
        this.startPoint = startPoint;
    }

    public TableHeaderBuilder getHeaderBuilder() {
        return headerBuilder;
    }

    public IDecisionTableColumnBuilder getConditionsBuilder() {
        return conditionsBuilder;
    }

    public IDecisionTableColumnBuilder getReturnBuilder() {
        return returnBuilder;
    }    

    public void setHeaderBuilder(TableHeaderBuilder headerBuilder) {
        this.headerBuilder = headerBuilder;
    }
    
    public void setConditionsBuilder(IDecisionTableColumnBuilder conditionsBuilder) {
        this.conditionsBuilder = conditionsBuilder;
    }

    public void setReturnBuilder(IDecisionTableColumnBuilder returnBuilder) {
        this.returnBuilder = returnBuilder;
    }
    
    /**
     * Builds the decision table on the given sheet with given number of rules.     
     * 
     * @param sheet sheet to build table on it.
     * @param rulesNumber number of rules for decision table
     * 
     * @return sheet with builded table on it.
     */
    public IWritableGrid build(IWritableGrid sheet, int rulesNumber) {        
        
        if (conditionsBuilder == null) {
            throw new OpenlNotCheckedException("Condition builder cannot be null when building decision table");
        }
        
        /**
         * column index that is free for further writing
         */
        int lastColumnIndex = conditionsBuilder.build(sheet, rulesNumber, startPoint.getColumn(), startPoint.getRow());
        
        if (returnBuilder == null) {
            throw new OpenlNotCheckedException("Return builder cannot be null when building decision table");
        }
        returnBuilder.build(sheet, rulesNumber, lastColumnIndex, startPoint.getRow());
        
        if (headerBuilder == null) {
            throw new OpenlNotCheckedException("Header builder cannot be null when building decision table");
        }
        
        headerBuilder.build(sheet, lastColumnIndex, startPoint.getColumn(), startPoint.getRow());        
        
        return sheet;
    }
    
}
