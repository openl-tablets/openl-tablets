package org.openl.rules.table.formatters;

import org.openl.util.formatters.IFormatter;

/**
 * @author Andrei Astrouski
 */
public class FormulaFormatter implements IFormatter {

    private IFormatter formulaResultFormatter;

    public FormulaFormatter() {
    }

    public FormulaFormatter(IFormatter formulaResultFormatter) {
        if (formulaResultFormatter instanceof FormulaFormatter) {
            throw new IllegalArgumentException();
        }
        this.formulaResultFormatter = formulaResultFormatter;
    }

    @Override
    public String format(Object value) {
        if (formulaResultFormatter != null) {
            return formulaResultFormatter.format(value);
        }
        return value != null ? value.toString() : null;
    }

    @Override
    public Object parse(String value) {
        return value;
    }
}
