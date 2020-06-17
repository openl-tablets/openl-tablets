package org.openl.rules.webstudio.web;

import java.util.UUID;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Request scope managed bean providing logic for Main page.
 */
@Service
@RequestScope
public class MainBean {

    private final RepositoryTreeState repositoryTreeState;

    private final Comments designRepoComments;

    private final CommentValidator commentValidator;

    private String requestId;

    private final Logger log = LoggerFactory.getLogger(MainBean.class);

    public MainBean(RepositoryTreeState repositoryTreeState,
        @Qualifier("designRepositoryComments") Comments designRepoComments) {
        if (WebContext.getContextPath() == null) {
            WebContext.setContextPath(WebStudioUtils.getExternalContext().getRequestContextPath());
        }
        requestId = UUID.randomUUID().toString();

        commentValidator = CommentValidator.forDesignRepo();
        this.repositoryTreeState = repositoryTreeState;
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

        String branchName = WebStudioUtils.getRequestParameter("branch");
        String projectName = WebStudioUtils.getRequestParameter("project");
        String moduleName = WebStudioUtils.getRequestParameter("module");

        studio.init(branchName, projectName, moduleName);
    }

    public String getVersionComment() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        RulesProject project = studio.getCurrentProject();

        if (project != null && project.isOpenedOtherVersion()) {
            FileData fileData = project.getFileData();
            return designRepoComments.restoredFrom(fileData.getVersion(), fileData.getAuthor(), fileData.getModifiedAt());
        }

        return designRepoComments.saveProject(project == null ? "" : project.getName());
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
        studio.saveProject(WebStudioUtils.getSession());
    }

    public void reload() {
        try {
            WebStudioUtils.getRulesUserSession().getUserWorkspace().refresh();
        } catch (CommonException e) {
            log.error("Error on reloading user's workspace", e);
        }
        repositoryTreeState.invalidateTree();
        repositoryTreeState.invalidateSelection();
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
