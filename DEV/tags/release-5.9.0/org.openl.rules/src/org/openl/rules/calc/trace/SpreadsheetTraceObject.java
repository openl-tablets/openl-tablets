package org.openl.rules.calc.trace;

import java.util.List;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.IGridRegion;
import org.openl.types.IOpenMethod;

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
    
    /** Is overriden to provide functionality not to write a result when it is represented as
     * SpreadsheetResult
     */
    @Override
    protected String getFormattedValue(Object value, IOpenMethod method) {
        if (!ClassUtils.isAssignable(method.getType().getInstanceClass(), SpreadsheetResult.class, false)) {
            // Write results when it is not a SpreadsheetResult, as it is very complex
            return super.getFormattedValue(value, method);
        }
        return StringUtils.EMPTY;
    }
}
