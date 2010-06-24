package org.openl.rules.webstudio.web;

import java.io.IOException;
import java.util.List;

import javax.faces.model.SelectItem;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.commons.web.util.WebTool;
import org.openl.rules.project.model.Module;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

import static org.openl.rules.webstudio.web.util.WebStudioUtils.getWebStudio;

/**
 * Request scope managed bean providing logic for header page of OpenL Studio.
 */
public class HeaderBean {

    private boolean hideLogout;

    public SelectItem[] getProjects() throws IOException {
        List<Module> modules = getWebStudio().getAllModules();
        SelectItem[] selectItems = new SelectItem[modules.size()];
        for (int i = 0; i < modules.size(); i++) {
            selectItems[i] = new SelectItem(modules.get(i).getClassname(), modules.get(i).getName());
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
        List<Module> modules = getWebStudio().getAllModules();
        return modules.size() > 0;
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
