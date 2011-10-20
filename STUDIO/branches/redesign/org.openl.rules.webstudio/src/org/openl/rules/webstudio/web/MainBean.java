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

        handleRequestParams();
    }

    /**
     * Stub method that used for bean initialization.
     */
    public String getInit() {
        return StringUtils.EMPTY;
    }

    private void handleRequestParams() throws Exception {
        WebStudio studio = WebStudioUtils.getWebStudio();

        String reload = FacesUtils.getRequestParameter("reload");
        if (reload != null) {
            studio.reset(ReloadType.valueOf(reload.toUpperCase()));
        }

        String operation = FacesUtils.getRequestParameter("operation");
        if (operation != null) {
            studio.executeOperation(operation, FacesUtils.getSession());
        }
    }

    public void selectModule() throws Exception {
        WebStudio studio = WebStudioUtils.getWebStudio();
        String projectId = FacesUtils.getRequestParameter("project");
        String moduleName = FacesUtils.getRequestParameter("module");
        studio.selectModule(projectId, moduleName);
    }

}
