package org.openl.rules.calc.trace;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ATableTracerLeaf;
import org.openl.rules.table.IGridRegion;

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

    public List<IGridRegion> getGridRegions() {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        regions.add(spreadsheetCell.getSourceCell().getAbsoluteRegion());
        return regions;
    }

    public String getType() {
        return SPREADSHEET_CELL_TYPE;
    }

}
