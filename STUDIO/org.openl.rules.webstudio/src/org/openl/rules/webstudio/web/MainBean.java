package org.openl.rules.webstudio.web;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.CommonException;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.jsf.WebContext;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request scope managed bean providing logic for Main page.
 */
@ManagedBean
@RequestScoped
public class MainBean {

    private final Logger log = LoggerFactory.getLogger(MainBean.class);
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

        studio.init(projectName, moduleName);
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
        try {
            WebStudioUtils.getRulesUserSession(FacesUtils.getSession()).getUserWorkspace().refresh();
        } catch (CommonException e) {
            log.error("Error on reloading user's workspace", e);
        }
        WebStudioUtils.getWebStudio().resetProjects();
    }
}
