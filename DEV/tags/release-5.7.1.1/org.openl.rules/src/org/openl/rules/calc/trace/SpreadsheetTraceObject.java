package org.openl.rules.calc.trace;

import java.util.List;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IGridRegion;

public class SpreadsheetTraceObject extends ATableTracerNode {

    private static final String SPREADSHEET_TYPE = "spreadsheet";

    public SpreadsheetTraceObject(Spreadsheet spreadsheet, Object[] params) {
        super(spreadsheet, params);
    }

    public Spreadsheet getSpreadsheet() {
        return (Spreadsheet) getTraceObject();
    }

    @Override
    public String getUri() {
        return getSpreadsheet().getSourceUrl();
    }

    public List<IGridRegion> getGridRegions() {
        return null;
    }

    public String getType() {
        return SPREADSHEET_TYPE;
    }

    public String getDisplayName(int mode) {
        return "SpreadSheet " + asString(getSpreadsheet(), mode);
    }

}
