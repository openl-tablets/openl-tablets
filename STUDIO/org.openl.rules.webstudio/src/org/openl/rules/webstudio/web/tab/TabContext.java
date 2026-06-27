package org.openl.rules.webstudio.web.tab;

import lombok.Getter;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.ProjectModel;

/**
 * The project and module that one browser tab is editing, resolved for a single request from the identity the
 * client sends with that request.
 *
 * <p>The legacy JSF UI historically kept one "current selection" per HTTP session, so a second tab editing a
 * different project clobbered the first. A tab context lets the JSF layer serve each request the tab's own
 * compiled model instead, so several tabs edit different projects at once within one session. A context is
 * request-scoped and read-only: identity, project, module and model are resolved once.
 */
@Getter
public class TabContext {

    private final String repositoryId;
    private final ProjectDescriptor projectDescriptor;
    private final RulesProject project;
    private final Module module;
    private final ProjectModel model;

    public TabContext(String repositoryId,
                      ProjectDescriptor projectDescriptor,
                      RulesProject project,
                      Module module,
                      ProjectModel model) {
        this.repositoryId = repositoryId;
        this.projectDescriptor = projectDescriptor;
        this.project = project;
        this.module = module;
        this.model = model;
    }
}
