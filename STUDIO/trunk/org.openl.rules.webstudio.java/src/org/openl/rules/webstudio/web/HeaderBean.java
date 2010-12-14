package org.openl.rules.webstudio.web;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.commons.web.util.WebTool;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

import static org.openl.rules.webstudio.web.util.WebStudioUtils.getWebStudio;

/**
 * Request scope managed bean providing logic for header page of OpenL Studio.
 */
public class HeaderBean {

    private boolean hideLogout;

    public List<SelectItem> getModules() {
        WebStudio studio = getWebStudio();
        List<ProjectDescriptor> projects = studio.getAllProjects();
        List<SelectItem> selectItems = new ArrayList<SelectItem>();
        for (ProjectDescriptor project : projects) {
            selectItems.add(new SelectItem(project.getId(), project.getName(), null, true));
            for (Module module : project.getModules()) {
                selectItems.add(new SelectItem(studio.getModuleId(module), module.getName()));
            }
        }
        return selectItems;
    }

    public String getSelectedModule() {
        WebStudio studio = getWebStudio();
        Module currentModule = studio.getCurrentModule();
        if (currentModule != null) {
            return studio.getModuleId(currentModule);
        }
        return "";
    }

    public boolean isHideLogout() {
        return hideLogout;
    }

    public boolean isLocalRequest() {
        return WebTool.isLocalRequest(FacesUtils.getRequest());
    }

    public boolean isProjectEditable() {
        WebStudio webStudio = WebStudioUtils.getWebStudio();
        return webStudio != null && webStudio.getModel().isEditable();
    }

    public boolean isProjectCompiledSuccessfully() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        return model.isProjectCompiledSuccessfully();
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

}
