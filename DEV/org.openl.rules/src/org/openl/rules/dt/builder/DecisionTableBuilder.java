package org.openl.rules.dt.builder;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.Point;
import org.openl.rules.validation.properties.dimentional.Builder;

/**
 * Creates the memory representation of DT table by POI.
 * 
 * @author DLiauchuk
 *
 */
public class DecisionTableBuilder implements Builder<IWritableGrid> {

    /** number 5 - is a number of first development rows in table. */
    static final int DECISION_TABLE_HEADER_ROWS_NUMBER = 5;
    static final int CONDITION_TITLE_ROW_INDEX = 4;
    static final int PARAMETER_DECLARATION_ROW_INDEX = 3;
    static final int CODE_EXPRESSION_ROW_INDEX = 2;

    /** condition name always is the next row after header row. */
    static final int COLUMN_TYPE_ROW_INDEX = 1;

    private TableHeaderBuilder headerBuilder;
    private IDecisionTableColumnBuilder conditionsBuilder;
    private IDecisionTableColumnBuilder returnBuilder;
    private Point startPoint;

    private IWritableGrid sheetWithTable;
    private int rulesNumber;

    public DecisionTableBuilder(Point startPoint) {
        this.startPoint = startPoint;
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

    public void setSheetWithTable(IWritableGrid sheetWithTable) {
        this.sheetWithTable = sheetWithTable;
    }

    public void setRulesNumber(int rulesNumber) {
        this.rulesNumber = rulesNumber;
    }

    /**
     * Builds the decision table on the given sheet with given number of rules.
     * 
     * @param sheet sheet to build table on it.
     * @param rulesNumber number of rules for decision table
     * 
     * @return sheet with builded table on it.
     * @deprecated
     */
    public IWritableGrid build(IWritableGrid sheet, int rulesNumber) {
        setSheetWithTable(sheet);
        setRulesNumber(rulesNumber);

        build();
        return sheetWithTable;
    }

    public IWritableGrid build() {
        if (conditionsBuilder == null) {
            throw new OpenlNotCheckedException("Condition builder cannot be null when building decision table");
        }

        // column index that is free for further writing
        int lastColumnIndex = conditionsBuilder
            .build(sheetWithTable, rulesNumber, startPoint.getColumn(), startPoint.getRow());

        if (returnBuilder == null) {
            throw new OpenlNotCheckedException("Return builder cannot be null when building decision table");
        }
        returnBuilder.build(sheetWithTable, rulesNumber, lastColumnIndex, startPoint.getRow());

        if (headerBuilder == null) {
            throw new OpenlNotCheckedException("Header builder cannot be null when building decision table");
        }

        headerBuilder.build(sheetWithTable, lastColumnIndex, startPoint.getColumn(), startPoint.getRow());

        return sheetWithTable;
    }

}
