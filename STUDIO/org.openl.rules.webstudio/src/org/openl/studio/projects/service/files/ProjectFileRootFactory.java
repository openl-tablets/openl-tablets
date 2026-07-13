package org.openl.studio.projects.service.files;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.studio.projects.validator.ProjectStateValidator;

/**
 * Builds a {@link FileRoot} over a project's working copy.
 *
 * @author Yury Molchan
 */
@Component
@RequiredArgsConstructor
public class ProjectFileRootFactory {

    private final AclProjectsHelper aclProjectsHelper;
    private final ProjectStateValidator projectStateValidator;
    private final ProjectFileLookupService fileLookupService;
    private final UserManagementService userManagementService;

    public FileRoot of(RulesProject project) {
        return new ProjectFileRoot(project, aclProjectsHelper, projectStateValidator, fileLookupService,
                () -> AuthoringRepository.currentAuthor(userManagementService));
    }
}
