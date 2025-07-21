package org.openl.rules.webstudio.web.repository.upload;

import jakarta.xml.bind.JAXBException;

import org.xml.sax.SAXParseException;

import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;

public final class ProjectDescriptorUtils {
    private ProjectDescriptorUtils() {
    }

    public static String getErrorMessage(JAXBException e) {
        StringBuilder message = new StringBuilder(
                "Cannot parse project descriptor file " + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME + '.');
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
