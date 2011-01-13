package org.openl.rules.table.xls.formatters;

//import org.openl.util.Log;

/**
 * @author Andrei Astrouski
 *
 */
public class XlsFormulaFormatter extends AXlsFormatter {
    //private static final String FORMULA_PREFIX = "=";
    private AXlsFormatter formulaResultFormatter;

    public XlsFormulaFormatter() {
    }

    public XlsFormulaFormatter(AXlsFormatter formulaResultFormatter) {
        if (formulaResultFormatter instanceof XlsFormulaFormatter) {
            throw new IllegalArgumentException();
        }
        this.formulaResultFormatter = formulaResultFormatter;
    }

    public String format(Object value) {
        if (formulaResultFormatter != null) {
            return formulaResultFormatter.format(value);
        }
        return value != null ? value.toString() : null;
    }
    
    public Object parse(String value) {
        /*if (!value.startsWith(FORMULA_PREFIX)) {
            Log.warn("Could not parse Formula: " + value);
        }*/
        return value;
    }

}
