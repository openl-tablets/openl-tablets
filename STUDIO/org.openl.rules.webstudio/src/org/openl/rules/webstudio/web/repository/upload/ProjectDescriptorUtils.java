package org.openl.rules.webstudio.web.repository.upload;

import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;

public final class ProjectDescriptorUtils {
    private ProjectDescriptorUtils() {
    }

    public static String getErrorMessage() {
        return "Cannot parse project descriptor file " +
                ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME + '.';
    }
}
