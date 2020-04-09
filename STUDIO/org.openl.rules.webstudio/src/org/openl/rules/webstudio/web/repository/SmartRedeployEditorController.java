package org.openl.rules.webstudio.web.repository;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.annotation.SessionScope;

/**
 * @author Aleh Bykhavets
 */
@Controller
@SessionScope
public class SmartRedeployEditorController extends AbstractSmartRedeployController {

    private boolean loading = true;

    public void setProject(String projectName) {
        try {
            reset();
            currentProject = userWorkspace.getProject(projectName);
            loading = false;
        } catch (ProjectException e) {
            log.warn("Failed to retrieve the project", e);
        }
    }

    public AProject getCurrentProject() {
        return currentProject;
    }

    public void reset() {
        setRepositoryConfigName(null);
        items = null;
        currentProject = null;
        loading = true;
    }

    public boolean isLoading() {
        return loading;
    }

    @Override
    public AProject getSelectedProject() {
        return getCurrentProject();
    }
}
