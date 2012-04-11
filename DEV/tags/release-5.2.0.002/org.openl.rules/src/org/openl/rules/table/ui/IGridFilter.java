/**
 * Created Mar 1, 2007
 */
package org.openl.rules.table.ui;

public interface IGridFilter
{
	IGridSelector getGridSelector();
	FormattedCell filterFormat(FormattedCell cell);
	
	Object parse(String value);
}