package org.openl.rules.table.ui.filters;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.xls.formatters.AXlsFormatter;

public class DateFilter extends AGridFilter{
    
    private AXlsFormatter format;
    
    public DateFilter(AXlsFormatter format) {
        this.format = format;
    }
    
    public FormattedCell filterFormat(FormattedCell cell) {
        Object value = cell.getObjectValue();
        if (value == null) {
            return cell;
        }

        String fDate = format.format(value);
        if (fDate == null) {
            return cell;
        }

        cell.setFormattedValue(fDate);
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
