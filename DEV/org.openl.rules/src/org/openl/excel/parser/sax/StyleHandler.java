package org.openl.excel.parser.sax;

import lombok.Getter;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class StyleHandler extends DefaultHandler {
    @Getter
    private final MinimalStyleTable styleTable = new MinimalStyleTable();

    private boolean startCellXfs;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("cellXfs".equals(localName)) {
            startCellXfs = true;
        } else if (startCellXfs && "xf".equals(localName)) {
            styleTable.addStyle(Integer.parseInt(attributes.getValue("numFmtId")));
        } else if ("numFmt".equals(localName)) {
            var numFmtId = Integer.parseInt(attributes.getValue("numFmtId"));
            var formatCode = attributes.getValue("formatCode");
            styleTable.addFormatString(numFmtId, formatCode);
        } else if (startCellXfs && "alignment".equals(localName)) {
            var indent = attributes.getValue("indent");
            if (indent != null) {
                styleTable.addIndent(Short.parseShort(indent));
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if ("cellXfs".equals(localName)) {
            startCellXfs = false;
        }
    }
}
