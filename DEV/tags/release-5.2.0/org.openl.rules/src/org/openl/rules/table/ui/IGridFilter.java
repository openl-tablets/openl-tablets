/**
 * Created Mar 1, 2007
 */
package org.openl.rules.table.ui;

public interface IGridFilter {
    FormattedCell filterFormat(FormattedCell cell);

    IGridSelector getGridSelector();

    Object parse(String value);
}