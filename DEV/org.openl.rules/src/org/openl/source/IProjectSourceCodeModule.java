package org.openl.source;

import java.nio.file.Path;

/**
 * A source that is a file of an OpenL project.
 *
 * <p>The place of a file in its project is the same wherever the project is opened: in the workspace of any user, on
 * any machine. What is named after that place keeps its name everywhere.
 *
 * @author Yury Molchan
 */
public interface IProjectSourceCodeModule extends IOpenSourceCodeModule {

    /**
     * The folder of the project this file belongs to.
     *
     * @return the project folder
     */
    Path getProjectFolder();
}
