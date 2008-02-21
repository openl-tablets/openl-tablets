/**
 * Created Mar 1, 2007
 */
package org.openl.rules.table.ui;

import org.openl.util.StringTool;

public class SimpleHtmlFilter extends AGridFilter
{

	public FormattedCell filterFormat(FormattedCell cell)
	{ 
		if (cell.style.isWrappedText())
			cell.content = StringTool.encodeHTMLBody(cell.content);
		else
			cell.content = StringTool.prepareXMLBodyValue(cell.content);
		return cell;
	}
	
}