package org.openl.rules.webstudio.web;

import java.util.Map;
import java.util.UUID;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.CommonException;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.ui.Explanator;
import org.openl.rules.ui.ParameterRegistry;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.jsf.WebContext;
import org.openl.rules.webstudio.web.repository.CommentValidator;
import org.openl.rules.webstudio.web.repository.RepositoryTreeState;
import org.openl.rules.webstudio.web.tableeditor.TableBean;
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

    @ManagedProperty(value = "#{systemConfig}")
    private Map<String, Object> config;

    @ManagedProperty(value = "#{designRepositoryComments}")
    private Comments designRepoComments;

    private CommentValidator commentValidator;

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

    public void setConfig(Map<String, Object> config) {
        this.config = config;

        commentValidator = CommentValidator.forDesignRepo(config);
    }

    public void setDesignRepoComments(Comments designRepoComments) {
        this.designRepoComments = designRepoComments;
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

        String branchName = FacesUtils.getRequestParameter("branch");
        String projectName = FacesUtils.getRequestParameter("project");
        String moduleName = FacesUtils.getRequestParameter("module");

        studio.init(branchName, projectName, moduleName);
    }


    public String getVersionComment() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        RulesProject project = studio.getCurrentProject();

        if (project != null && project.isOpenedOtherVersion()) {
            return designRepoComments.restoredFrom(project.getHistoryVersion());
        }


        return designRepoComments.saveProject();
    }

    public void setVersionComment(String comment) {
        WebStudio studio = WebStudioUtils.getWebStudio();
        RulesProject project = studio.getCurrentProject();
        if (project != null) {
            FileData fileData = project.getFileData();
            if (fileData != null) {
                fileData.setComment(comment);
            }
        }
    }

    public void commentValidator(FacesContext context, UIComponent toValidate, Object value) {
        String comment = (String) value;

        commentValidator.validate(comment);
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
            ParameterRegistry.remove(requestId);
            TableBean.tryUnlock(requestId);
        }
    }
}
