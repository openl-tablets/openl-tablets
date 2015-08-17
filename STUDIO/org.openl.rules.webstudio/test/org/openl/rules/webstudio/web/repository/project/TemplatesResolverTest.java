package org.openl.rules.webstudio.web.repository.project;

import org.openl.util.IOUtils;

/**
 * Base class with helper methods
 *
 * @author nsamatov.
 */
public abstract class TemplatesResolverTest {
    protected void close(ProjectFile[] projectFiles) {
        for (ProjectFile projectFile : projectFiles) {
            IOUtils.closeQuietly(projectFile.getInput());
        }
    }

    protected boolean contains(ProjectFile[] projectFiles, String filename) {
        for (ProjectFile projectFile : projectFiles) {
            if (projectFile.getName().equals(filename)) {
                return true;
            }
        }
        return false;
    }
}
