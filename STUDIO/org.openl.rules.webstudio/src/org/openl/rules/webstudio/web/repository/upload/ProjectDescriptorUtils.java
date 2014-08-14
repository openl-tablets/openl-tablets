package org.openl.rules.webstudio.web.repository.upload;

import com.thoughtworks.xstream.XStreamException;
import org.xml.sax.SAXParseException;

public final class ProjectDescriptorUtils {
    private ProjectDescriptorUtils() {}

    public static String getErrorMessage(XStreamException e) {
        StringBuilder message = new StringBuilder("Can't parse rules.xml.");
        if (e.getCause() instanceof SAXParseException) {
            SAXParseException parseException = (SAXParseException) e.getCause();
            message.append(" Line number: ").append(parseException.getLineNumber())
                    .append(", column number: ").append(parseException.getColumnNumber())
                    .append(".");
        }
        return message.toString();
    }
}
