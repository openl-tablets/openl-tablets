/**
 * Created Mar 1, 2007
 */
package org.openl.rules.table.ui.filters;

import org.apache.commons.lang.StringEscapeUtils;
import org.openl.rules.table.FormattedCell;

@Deprecated
public class SimpleHtmlFilter extends AGridFilter {

    public FormattedCell filterFormat(FormattedCell cell) {
        String escapedStr = StringEscapeUtils.escapeHtml(cell.getFormattedValue());
        cell.setFormattedValue(escapedStr);
        return cell;
    }

}