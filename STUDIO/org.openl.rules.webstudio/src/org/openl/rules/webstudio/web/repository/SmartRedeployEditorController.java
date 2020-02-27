package org.openl.rules.webstudio.web.repository;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;

/**
 * @author Aleh Bykhavets
 */
@ManagedBean
@SessionScoped
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
