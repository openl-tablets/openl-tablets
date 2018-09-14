package org.openl.rules.webstudio.web;

import java.util.UUID;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.CommonException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.ui.Explanator;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.jsf.WebContext;
import org.openl.rules.webstudio.web.repository.RepositoryTreeState;
import org.openl.rules.webstudio.web.util.Constants;
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

    @ManagedProperty(value = "#{repositoryTreeState}")
    private RepositoryTreeState repositoryTreeState;

    private String requestId;

    private final Logger log = LoggerFactory.getLogger(MainBean.class);
    public MainBean() {
        if (WebContext.getContextPath() == null) {
            WebContext.setContextPath(FacesUtils.getContextPath());
        }
        requestId = UUID.randomUUID().toString();
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    /**
     * Stub method that used for bean initialization.
     */
    public String getInit() {
        WebStudioUtils.getWebStudio(true);
        return StringUtils.EMPTY;
    }

    public void init() {
        WebStudio studio = WebStudioUtils.getWebStudio(true);

        String projectName = FacesUtils.getRequestParameter("project");
        String moduleName = FacesUtils.getRequestParameter("module");

        studio.init(projectName, moduleName);
    }


    public String getVersionComment() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        RulesProject project = studio.getCurrentProject();

        if (project != null && project.isOpenedOtherVersion()) {
            return Constants.RESTORED_FROM_REVISION_PREFIX + project.getHistoryVersion();
        }


        return "";
    }

    public void setVersionComment(String comment) {
        WebStudio studio = WebStudioUtils.getWebStudio();
        RulesProject project = studio.getCurrentProject();
        if (project != null) {
            project.setVersionComment(comment);
        }
    }

    public void saveProject() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        studio.saveProject(FacesUtils.getSession());
    }

    public void reload() {
        try {
            WebStudioUtils.getRulesUserSession(FacesUtils.getSession()).getUserWorkspace().refresh();
        } catch (CommonException e) {
            log.error("Error on reloading user's workspace", e);
        }
        repositoryTreeState.invalidateTree();
        WebStudioUtils.getWebStudio().resetProjects();
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void onPageUnload() {
        if (StringUtils.isNotEmpty(requestId)) {
            log.debug("Page unload for request id: {}", requestId);
            Explanator.remove(requestId);
        }
    }
}
