package org.openl.rules.table.ui.filters;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.xls.formatters.AXlsFormatter;

public class FormatFilter extends AGridFilter {

    private AXlsFormatter formatter;

    public FormatFilter(AXlsFormatter format) {
        this.formatter = format;
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

    public AXlsFormatter getFormatter() {
        return this.formatter;
    }

    @Override
    public Object parse(String value) {
        return formatter.parse(value);
    }

}
