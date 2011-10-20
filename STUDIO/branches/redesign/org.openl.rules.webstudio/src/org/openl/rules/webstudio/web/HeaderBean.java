package org.openl.rules.webstudio.web;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.model.SelectItem;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

import static org.openl.rules.webstudio.web.util.WebStudioUtils.getWebStudio;

/**
 * Request scope managed bean providing logic for header page of OpenL Studio.
 */
@ManagedBean
@RequestScoped
public class HeaderBean {

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

    public boolean isRepositoryFailed() {
        return false;//WebStudioUtils.isRepositoryFailed();
    }

}
