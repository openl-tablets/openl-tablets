package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.table.ATableTracerNode;

public class SpreadsheetTraceObject extends ATableTracerNode {

    public SpreadsheetTraceObject(Spreadsheet spreadsheet, Object[] params) {
        super("spreadsheet", "SpreadSheet", spreadsheet, params);
    }
}
