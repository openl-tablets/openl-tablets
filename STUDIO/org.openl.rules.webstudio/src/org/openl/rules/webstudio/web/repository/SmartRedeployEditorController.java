package org.openl.rules.webstudio.web.repository;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

/**
 * @author Aleh Bykhavets
 */
@Service
@SessionScope
public class SmartRedeployEditorController extends AbstractSmartRedeployController {
    private String repositoryId;

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public void setProject(String projectName) {
        try {
            String repoId = repositoryId;
            reset();
            currentProject = userWorkspace.getProject(repoId, projectName);

            repositoryId = null;
        } catch (ProjectException e) {
            log.warn("Failed to retrieve the project", e);
        }
    }

    public void reset() {
        setRepositoryConfigName(null);
        items = null;
        repositoryId = null;
        currentProject = null;
    }

    @Override
    public AProject getSelectedProject() {
        return currentProject;
    }
}
