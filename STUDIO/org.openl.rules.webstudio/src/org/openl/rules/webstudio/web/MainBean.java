package org.openl.rules.webstudio.web;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.OpenL;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.conf.OpenLConfiguration;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.jsf.WebContext;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

/**
 * Request scope managed bean providing logic for Main page.
 */
@ManagedBean
@RequestScoped
public class MainBean {

    public MainBean() throws Exception {
        if (WebContext.getContextPath() == null) {
            WebContext.setContextPath(FacesUtils.getContextPath());
        }
    }

    /**
     * Stub method that used for bean initialization.
     */
    public String getInit() {
    	WebStudioUtils.getWebStudio(true);
        return StringUtils.EMPTY;
    }

    public void init() throws Exception {
        WebStudio studio = WebStudioUtils.getWebStudio(true);

        String projectName = FacesUtils.getRequestParameter("project");
        String moduleName = FacesUtils.getRequestParameter("module");

        if (StringUtils.isBlank(projectName) && StringUtils.isBlank(moduleName)) {
            // Clear project/module on Home page
            studio.setCurrentModule(null);
            return;
        }

        if (StringUtils.isNotBlank(projectName)) {
            ProjectDescriptor project = studio.getCurrentProjectDescriptor();

            if (StringUtils.isNotBlank(moduleName)) {
                synchronized (WebStudioUtils.getWebStudio()) {
                    // Select module
                    Module module = studio.getCurrentModule();
                    if (project != null && module != null
                            && !project.getName().equals(projectName)
                            && !module.getName().equals(moduleName)) {
                        // Delete all previous cached config
                        OpenL.reset();
                        OpenLConfiguration.reset();
                    }
                    studio.selectModule(projectName, moduleName);
                }
            } else {
                // Select project
                studio.selectProject(projectName);
            }
        }
    }

    public void saveProject() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        studio.saveProject(FacesUtils.getSession());
    }

    public void editProject() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        studio.editProject(FacesUtils.getSession());
    }

    public void reload() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        studio.reset(ReloadType.FORCED);
        studio.getModel().getProjectTree(); // Reason: tree should be built
                                            // before accessing the ProjectModel.
                                            // Is is related to UI: rendering of
                                            // frames is asynchronous and we
                                            // should build tree before the
                                            // 'content' frame
    }

}
