/**
 * Created Mar 1, 2007
 */
package org.openl.rules.table.ui.filters;

import org.openl.rules.table.FormattedCell;
import org.openl.util.StringTool;

public class SimpleHtmlFilter extends AGridFilter {

    public FormattedCell filterFormat(FormattedCell cell) {
        if (cell.getStyle().isWrappedText()) {
            cell.setFormattedValue(StringTool.encodeHTMLBody(cell.getFormattedValue()));
        } else {
            cell.setFormattedValue(StringTool.prepareXMLBodyValue(cell.getFormattedValue()));
        }
        return cell;
    }

}