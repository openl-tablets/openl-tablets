package org.openl.rules.webstudio.web;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.commons.lang.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.project.instantiation.ReloadType;
//import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.jsf.WebContext;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

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

    public void checkInProject() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        studio.checkInProject(FacesUtils.getSession());
    }

    public void checkOutProject() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        studio.checkOutProject(FacesUtils.getSession());
    }

    public void reload() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        studio.reset(ReloadType.FORCED);
        if (studio.getCurrentModule() != null) {
            studio.getModel().getProjectTree();// Reason: tree should be built
                                               // before accessing the ProjectModel.
                                               // Is is related to UI: rendering of
                                               // frames is asynchronous and we
                                               // should build tree before the
                                               // 'content' frame
        }
    }

    public void selectModule() throws Exception {
        WebStudio studio = WebStudioUtils.getWebStudio();
        String projectId = FacesUtils.getRequestParameterClean("project");
        String moduleName = FacesUtils.getRequestParameterClean("module");
        studio.selectModule(projectId, moduleName);
    }

    public void clearModule() throws Exception {
        WebStudio studio = WebStudioUtils.getWebStudio();
        studio.setCurrentModule(null);
    }

}
