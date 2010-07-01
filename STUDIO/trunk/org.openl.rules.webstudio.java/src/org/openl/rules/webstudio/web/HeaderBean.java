package org.openl.rules.webstudio.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.commons.web.util.WebTool;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

import static org.openl.rules.webstudio.web.util.WebStudioUtils.getWebStudio;

/**
 * Request scope managed bean providing logic for header page of OpenL Studio.
 */
public class HeaderBean {

    private boolean hideLogout;

    public List<SelectItem> getProjects() throws IOException {
        List<ProjectDescriptor> projects = getWebStudio().getAllProjects();
        List<SelectItem> selectItems = new ArrayList<SelectItem>();
        for (ProjectDescriptor project : projects) {
            selectItems.add(new SelectItem(project.getName(), project.getName(), null, true));
            for (Module module : project.getModules()) {
                selectItems.add(new SelectItem(module.getClassname(), module.getName()));
            }
        }
        return selectItems;
    }

    public String getSelectedProject() {
        Module current = getWebStudio().getCurrentModule();
        if (current != null) {
            return current.getClassname();
        }
        return "";
    }

    public boolean isHideLogout() {
        return hideLogout;
    }

    public boolean isLocalRequest() {
        return WebTool.isLocalRequest(FacesUtils.getRequest());
    }

    public boolean isProjectReadOnly() {
        WebStudio webStudio = WebStudioUtils.getWebStudio();
        return webStudio == null || webStudio.getModel().isReadOnly();
    }

    public boolean isProjectsExist() throws IOException {
        List<ProjectDescriptor> projects = getWebStudio().getAllProjects();
        return projects.size() > 0;
    }

    public boolean isRepositoryFailed() {
        return WebStudioUtils.isRepositoryFailed();
    }

    public boolean isShowFormulas() {
        WebStudio webStudio = WebStudioUtils.getWebStudio();
        return webStudio == null || webStudio.isShowFormulas();
    }
    
    public boolean isCollapseProperties() {
        WebStudio webStudio = WebStudioUtils.getWebStudio();
        return webStudio == null || webStudio.isCollapseProperties();
    }

    public void setHideLogout(boolean hideLogout) {
        this.hideLogout = hideLogout;
    }

    public void setSelectedProject(String selectedProject) throws Exception {
        // Actual select happens now in a different place.

        // getWebStudio().select(selectedProject);
    }

}
