/**
 * Created Mar 1, 2007
 */
package org.openl.rules.table.ui;

import org.openl.rules.table.FormattedCell;

public interface IGridFilter {
    FormattedCell filterFormat(FormattedCell cell);

    IGridSelector getGridSelector();

    Object parse(String value);
}