package org.openl.rules.webstudio.web.repository;

import org.springframework.stereotype.Service;

import org.openl.rules.common.ProjectException;
import org.openl.rules.webstudio.web.jsf.annotation.ViewScope;

/**
 * @author Aleh Bykhavets
 */
@Service
@ViewScope
public class SmartRedeployController extends AbstractSmartRedeployController {

    private String repositoryId;
    private String currentProjectName;

    @Override
    public void reset() {
        setRepositoryConfigName(null);
        items = null;
        currentProject = null;
    }

    public void initProject() throws ProjectException {
        reset();
        currentProject = userWorkspace.getProject(repositoryId, currentProjectName, false);
        setDeployComment(null);
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getCurrentProjectName() {
        return currentProjectName;
    }

    public void setCurrentProjectName(String currentProjectName) {
        this.currentProjectName = currentProjectName;
    }
}
