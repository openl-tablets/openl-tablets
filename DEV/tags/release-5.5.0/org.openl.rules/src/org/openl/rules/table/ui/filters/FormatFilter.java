package org.openl.rules.table.ui.filters;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.xls.formatters.AXlsFormatter;

public class FormatFilter extends AGridFilter {

    private AXlsFormatter format;
    
    public FormatFilter(AXlsFormatter format) {
        this.format = format;
    }
    
    public FormattedCell filterFormat(FormattedCell cell) {
        Object value = cell.getObjectValue();
        if (value == null) {
            return cell;
        }
        cell.setFormattedValue(format.format(value));
        cell.setFilter(this);

        return cell;
    }

    public AXlsFormatter getFormatter() {
        return this.format;
    }
    
    @Override
    public Object parse(String value) {
        return format.parse(value);
    }

}
