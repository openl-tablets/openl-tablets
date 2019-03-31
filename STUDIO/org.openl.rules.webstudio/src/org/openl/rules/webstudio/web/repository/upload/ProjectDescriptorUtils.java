package org.openl.rules.webstudio.web.repository.upload;

import com.thoughtworks.xstream.XStreamException;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.xml.sax.SAXParseException;

public final class ProjectDescriptorUtils {
    private ProjectDescriptorUtils() {
    }

    public static String getErrorMessage(XStreamException e) {
        StringBuilder message = new StringBuilder(
            "Can't parse project descriptor file " + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME + '.');
        if (e.getCause() instanceof SAXParseException) {
            SAXParseException parseException = (SAXParseException) e.getCause();
            message.append(" Line number: ")
                .append(parseException.getLineNumber())
                .append(", column number: ")
                .append(parseException.getColumnNumber())
                .append(".");
        }
        return message.toString();
    }
}
