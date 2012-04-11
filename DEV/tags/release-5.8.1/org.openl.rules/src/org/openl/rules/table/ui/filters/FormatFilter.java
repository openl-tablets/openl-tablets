package org.openl.rules.table.ui.filters;

import org.openl.rules.table.FormattedCell;
import org.openl.util.formatters.IFormatter;

public class FormatFilter extends AGridFilter {

    private IFormatter formatter;

    public FormatFilter(IFormatter formatter) {
        this.formatter = formatter;
    }

    public FormattedCell filterFormat(FormattedCell cell) {
        Object value = cell.getObjectValue();
        if (value == null) {
            return cell;
        }

        String formattedValue = formatter.format(value);

        cell.setFormattedValue(formattedValue);
        cell.setFilter(this);

        return cell;
    }

    public IFormatter getFormatter() {
        return this.formatter;
    }

}
