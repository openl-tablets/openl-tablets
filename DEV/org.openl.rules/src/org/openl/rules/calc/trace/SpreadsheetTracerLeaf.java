package org.openl.rules.calc.trace;

import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.table.ATableTracerLeaf;

/**
 * Leaf trace object that represented by one calculation of spreadsheet cell
 *
 * @author PUdalau
 */
public class SpreadsheetTracerLeaf extends ATableTracerLeaf {
    private static final String SPREADSHEET_CELL_TYPE = "spreadsheetCell";
    private SpreadsheetTraceObject spreadsheetTraceObject;
    private SpreadsheetCell spreadsheetCell;

    public SpreadsheetTracerLeaf(SpreadsheetTraceObject spreadsheetTraceObject, SpreadsheetCell spreadsheetCell) {
        super("spreadsheetCell");
        this.spreadsheetTraceObject = spreadsheetTraceObject;
        this.spreadsheetCell = spreadsheetCell;
    }

    public SpreadsheetCell getSpreadsheetCell() {
        return spreadsheetCell;
    }

    public SpreadsheetTraceObject getSpreadsheetTraceObject() {
        return spreadsheetTraceObject;
    }

    @Override
    public String getUri() {
        return spreadsheetTraceObject.getTraceObject().getSyntaxNode().getUri();
    }

    public String getType() {
        return SPREADSHEET_CELL_TYPE;
    }

}
