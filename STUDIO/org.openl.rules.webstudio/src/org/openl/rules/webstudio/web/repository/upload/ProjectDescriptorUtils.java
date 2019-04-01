package org.openl.rules.webstudio.web.repository.upload;

import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.xml.sax.SAXParseException;

import com.thoughtworks.xstream.XStreamException;

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
