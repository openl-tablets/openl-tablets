package org.openl.rules.table.xls;

import org.openl.rules.table.ui.FormattedCell;
import org.openl.util.Log;

/**
 * @author Andrei Astrouski
 *
 */
public class XlsFormulaFormat extends XlsFormat {
    private static final String FORMULA_PREFIX = "=";
    private XlsFormat formulaResultFormatter;

    public XlsFormulaFormat() {
    }

    public XlsFormulaFormat(XlsFormat formulaResultFormatter) {
        if (formulaResultFormatter instanceof XlsFormulaFormat) {
            throw new IllegalArgumentException();
        }
        this.formulaResultFormatter = formulaResultFormatter;
    }

    public FormattedCell filterFormat(FormattedCell cell) {
        if (cell.value == null) {
            return cell;
        }
        cell.content = format(cell.value);
        cell.setFilter(this);

        return cell;
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
        if (!value.startsWith(FORMULA_PREFIX)) {
            Log.warn("Could not parse Formula: " + value);
        }
        return value;
    }

}
