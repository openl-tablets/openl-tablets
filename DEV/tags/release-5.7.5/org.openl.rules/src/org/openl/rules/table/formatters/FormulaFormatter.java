package org.openl.rules.table.formatters;

import org.openl.util.formatters.IFormatter;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

/**
 * @author Andrei Astrouski
 *
 */
public class FormulaFormatter implements IFormatter {

    //private static final Log LOG = LogFactory.getLog(XlsFormulaFormatter.class);

    //private static final String FORMULA_PREFIX = "=";
    private IFormatter formulaResultFormatter;

    public FormulaFormatter() {
    }

    public FormulaFormatter(IFormatter formulaResultFormatter) {
        if (formulaResultFormatter instanceof FormulaFormatter) {
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
            LOG.warn("Could not parse Formula: " + value);
        }*/
        return value;
    }

}
