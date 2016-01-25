package org.openl.rules.calc.trace;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.table.ATableTracerNode;

public class SpreadsheetTraceObject extends ATableTracerNode {

    public SpreadsheetTraceObject(Spreadsheet spreadsheet, Object[] params) {
        super("spreadsheet", "SpreadSheet", spreadsheet, params);
    }

    public Spreadsheet getSpreadsheet() {
        return (Spreadsheet) getTraceObject();
    }

}
