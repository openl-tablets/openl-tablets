package org.openl.excel.parser.sax;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class WorkbookHandler extends DefaultHandler {
    @Getter
    private final List<SAXSheetDescriptor> sheetDescriptors = new ArrayList<>();
    // The default value for attribute date1904 is false.
    @Getter
    private boolean use1904Windowing = false;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("sheet".equals(localName)) {
            var name = attributes.getValue("name");

            var rIdQName = "r:id";
            if (attributes.getIndex(rIdQName) < 0) {
                for (var i = 0; i < attributes.getLength(); i++) {
                    if ("id".equals(attributes.getLocalName(i))) {
                        rIdQName = attributes.getQName(i);
                        break;
                    }
                }
            }

            var referenceId = attributes.getValue(rIdQName);
            sheetDescriptors.add(new SAXSheetDescriptor(name, sheetDescriptors.size(), referenceId));
        } else if ("workbookPr".equals(localName)) {
            var date1904 = attributes.getValue("date1904");
            if (date1904 != null && isTrue(date1904)) {
                // If the dateCompatibility attribute is 0 or false, this attribute is ignored.
                // By default dateCompatibility is true.
                var dateCompatibility = attributes.getValue("dateCompatibility");
                if (dateCompatibility == null || isTrue(dateCompatibility)) {
                    use1904Windowing = true;
                }
            }
        }
    }

    private boolean isTrue(String boolVariable) {
        return "1".equals(boolVariable) || "true".equals(boolVariable);
    }
}
