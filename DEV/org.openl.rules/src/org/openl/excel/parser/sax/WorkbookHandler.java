package org.openl.excel.parser.sax;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class WorkbookHandler extends DefaultHandler {
    private final List<SAXSheetDescriptor> sheetDescriptors = new ArrayList<>();
    // The default value for attribute date1904 is false.
    private boolean use1904Windowing = false;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("sheet".equals(localName)) {
            String name = attributes.getValue("name");

            String rIdQName = "r:id";
            if (attributes.getIndex(rIdQName) < 0) {
                for (int i = 0; i < attributes.getLength(); i++) {
                    if ("id".equals(attributes.getLocalName(i))) {
                        rIdQName = attributes.getQName(i);
                        break;
                    }
                }
            }

            String referenceId = attributes.getValue(rIdQName);
            sheetDescriptors.add(new SAXSheetDescriptor(name, sheetDescriptors.size(), referenceId));
        } else if ("workbookPr".equals(localName)) {
            String date1904 = attributes.getValue("date1904");
            if (date1904 != null && isTrue(date1904)) {
                // If the dateCompatibility attribute is 0 or false, this attribute is ignored.
                // By default dateCompatibility is true.
                String dateCompatibility = attributes.getValue("dateCompatibility");
                if (dateCompatibility == null || isTrue(dateCompatibility)) {
                    use1904Windowing = true;
                }
            }
        }
    }

    private boolean isTrue(String boolVariable) {
        return "1".equals(boolVariable) || "true".equals(boolVariable);
    }

    public List<SAXSheetDescriptor> getSheetDescriptors() {
        return sheetDescriptors;
    }

    public boolean isUse1904Windowing() {
        return use1904Windowing;
    }
}
