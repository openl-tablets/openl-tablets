package org.openl.rules.webstudio.web;

import java.util.Optional;
import java.util.UUID;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;
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
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Request scope managed bean providing logic for Main page.
 */
@Service
@RequestScope
public class MainBean {

    private final RepositoryTreeState repositoryTreeState;

    private final PropertyResolver propertyResolver;

    private String requestId;

    private final Logger log = LoggerFactory.getLogger(MainBean.class);

    public MainBean(RepositoryTreeState repositoryTreeState, PropertyResolver propertyResolver) {
        if (WebContext.getContextPath() == null) {
            WebContext.setContextPath(WebStudioUtils.getExternalContext().getRequestContextPath());
        }
        requestId = UUID.randomUUID().toString();

        this.repositoryTreeState = repositoryTreeState;
        this.propertyResolver = propertyResolver;
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

        String repositoryId = WebStudioUtils.getRequestParameter("repositoryId");
        String branchName = WebStudioUtils.getRequestParameter("branch");
        String projectName = WebStudioUtils.getRequestParameter("project");
        String moduleName = WebStudioUtils.getRequestParameter("module");

        studio.init(repositoryId, branchName, projectName, moduleName);
    }

    public String getVersionComment() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        RulesProject project = studio.getCurrentProject();

        if (project == null || project.getDesignRepository() == null) {
            return null;
        }
        Comments designRepoComments = new Comments(propertyResolver, project.getDesignRepository().getId());

        if (project.isOpenedOtherVersion()) {
            FileData fileData = project.getFileData();
            String name = Optional.ofNullable(fileData.getAuthor()).map(UserInfo::getName).orElse(null);
            return designRepoComments.restoredFrom(fileData.getVersion(), name, fileData.getModifiedAt());
        }

        return designRepoComments.saveProject(project.getName());
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

        RulesProject project = WebStudioUtils.getWebStudio().getCurrentProject();
        if (project != null && project.getDesignRepository() != null) {
            CommentValidator.forRepo(project.getDesignRepository().getId()).validate(comment);
        }
    }

    public void saveProject() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        studio.saveProject(WebStudioUtils.getSession());
    }

    public void reload() {
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
