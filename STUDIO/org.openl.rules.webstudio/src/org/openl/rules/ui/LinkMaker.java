package org.openl.rules.ui;

import org.openl.meta.explanation.ExplanationNumberValue;
import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.ui.filters.AGridFilter;
import org.openl.rules.table.xls.formatters.XlsDataFormatterFactory;

/**
 * @author Yury Molchan
 */
class LinkMaker extends AGridFilter {
    static LinkMaker INSTANCE = new LinkMaker();

    public FormattedCell filterFormat(FormattedCell cell) {
        Object value = cell.getObjectValue();
        if (value instanceof ExplanationNumberValue<?>) {
            int rootID = Explanator.getUniqueId((ExplanationNumberValue<?>) value);
            String url = "javascript: explain(\'?rootID=" + rootID + "')";
            cell.setFormattedValue("<a href=\"" + url + "\">" + XlsDataFormatterFactory.getFormattedValue(cell,
                    cell.getMetaInfo()) + "</a>");
        }
        return cell;
    }
}
