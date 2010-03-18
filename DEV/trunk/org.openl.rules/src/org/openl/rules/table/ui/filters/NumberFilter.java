package org.openl.rules.table.ui.filters;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.formatters.AXlsFormatter;
import org.openl.rules.table.xls.formatters.SegmentFormatter;
import org.openl.rules.table.xls.formatters.XlsNumberFormatter;
import org.openl.util.Log;

public class NumberFilter extends AGridFilter {
    
    private AXlsFormatter format;
    
    public NumberFilter(AXlsFormatter format) {
        this.format = format;
    }
    
    public FormattedCell filterFormat(FormattedCell formattedCell) {
        Object cellValue = formattedCell.getObjectValue();
        if (cellValue == null) {
            return formattedCell;
        }

        if (cellValue instanceof String) {
            Log.error("Should be Number " + cellValue);
            return formattedCell;
        }

        Number value = (Number) cellValue;
        if (format instanceof XlsNumberFormatter) {
            SegmentFormatter segmentFormatter = ((XlsNumberFormatter)format).getSegmentFormatter(value);
            formattedCell.setFormattedValue(((XlsNumberFormatter)format).format(value, segmentFormatter));

            if (formattedCell.getFont().getFontColor() == null) {
                formattedCell.getFont().setFontColor(segmentFormatter.getColor());
            }

            if (formattedCell.getStyle().getHorizontalAlignment() == ICellStyle.ALIGN_GENERAL) {
                formattedCell.getStyle().setHorizontalAlignment(segmentFormatter.getAlignment());
            }

            formattedCell.setFilter(this);
        } else {
            throw new NumberFormatException("Foematter is not number");
        }
        
        return formattedCell;
    }

    public AXlsFormatter getFormatter() {        
        return format;
    }
    
    @Override
    public Object parse(String value) {
        return format.parse(value);
    }

}
