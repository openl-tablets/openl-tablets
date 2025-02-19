package org.openl.rules.webstudio.web.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.common.ProjectException;

/**
 * @author Aleh Bykhavets
 */
@Service
@SessionScope
public class SmartRedeployEditorController extends AbstractSmartRedeployController {
    private static final Logger LOG = LoggerFactory.getLogger(SmartRedeployEditorController.class);
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
            LOG.warn("Failed to retrieve the project", e);
        }
    }

    public void reset() {
        setRepositoryConfigName(null);
        items = null;
        repositoryId = null;
        currentProject = null;
        setDeployComment(null);
    }
}
