package org.openl.rules.calc.trace;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
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
        super(spreadsheetCell);
        this.spreadsheetTraceObject = spreadsheetTraceObject;
        this.spreadsheetCell = spreadsheetCell;
    }

    public Object getValue() {
        return result;
    }

    public void setValue(Object value) {
        this.result = value;
    }

    @Override
    public String getUri() {
        return spreadsheetCell.getSourceCell().getUri();
    }

    public List<IGridRegion> getGridRegions() {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        regions.add(spreadsheetCell.getSourceCell().getAbsoluteRegion());
        return regions;
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return spreadsheetTraceObject.getTableSyntaxNode();
    }

    public String getType() {
        return SPREADSHEET_CELL_TYPE;
    }

    public String getDisplayName(int mode) {
        StringBuilder buf = new StringBuilder(64);
        Spreadsheet spreadsheet = spreadsheetTraceObject.getSpreadsheet();
        buf.append(String.format("%s%s", SpreadsheetStructureBuilder.DOLLAR_SIGN, spreadsheet.getRowNames()[spreadsheetCell.getRowIndex()]));
        buf.append(String.format("%s%s", SpreadsheetStructureBuilder.DOLLAR_SIGN, spreadsheet.getColumnNames()[spreadsheetCell.getColumnIndex()]));
        buf.append(" = ").append(getStringResult());
        return buf.toString();
    }

    private String getStringResult() {        
        return String.valueOf(result);
    }
}
