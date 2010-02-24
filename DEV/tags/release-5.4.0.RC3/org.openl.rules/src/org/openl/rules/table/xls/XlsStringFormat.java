package org.openl.rules.table.xls;

import org.openl.rules.table.FormattedCell;

public class XlsStringFormat extends XlsFormat {

    @Override
    public String format(Object value) {
        return value.toString();
    }

    @Override
    public Object parse(String value) {
        return value;
    }

    public FormattedCell filterFormat(FormattedCell cell) {
        return cell;
    }

}
