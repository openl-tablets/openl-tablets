package org.openl.excel.parser.sax;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class StyleHandler extends DefaultHandler {
    private MinimalStyleTable styleTable = new MinimalStyleTable();

    private boolean startCellXfs = false;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("cellXfs".equals(localName)) {
            startCellXfs = true;
        } else if (startCellXfs && "xf".equals(localName)) {
            styleTable.addStyle(Integer.parseInt(attributes.getValue("numFmtId")));
        } else if ("numFmt".equals(localName)) {
            int numFmtId = Integer.parseInt(attributes.getValue("numFmtId"));
            String formatCode = attributes.getValue("formatCode");
            styleTable.addFormatString(numFmtId, formatCode);
        } else if (startCellXfs && "alignment".equals(localName)) {
            String indent = attributes.getValue("indent");
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

    public MinimalStyleTable getStyleTable() {
        return styleTable;
    }
}
