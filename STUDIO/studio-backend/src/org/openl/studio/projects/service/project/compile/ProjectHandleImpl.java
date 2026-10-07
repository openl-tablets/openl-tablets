package org.openl.studio.projects.service.project.compile;

import org.openl.rules.ui.ProjectModel;

/**
 * Plain record implementation of {@link ProjectHandle}.
 *
 * @author Vladyslav Pikus
 */
record ProjectHandleImpl(ProjectModel project, CompilationJob compilation) implements ProjectHandle {
}
