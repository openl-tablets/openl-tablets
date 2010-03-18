/**
 * Created Mar 1, 2007
 */
package org.openl.rules.table.ui.filters;

import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.ui.IGridSelector;
import org.openl.rules.table.xls.formatters.AXlsFormatter;

public interface IGridFilter {
    FormattedCell filterFormat(FormattedCell cell);

    IGridSelector getGridSelector();

    Object parse(String value);
    
    AXlsFormatter getFormatter();
}