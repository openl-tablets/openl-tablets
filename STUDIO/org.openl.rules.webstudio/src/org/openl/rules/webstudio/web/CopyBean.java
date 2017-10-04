package org.openl.rules.webstudio.web;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.CREATE_PROJECTS;

import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ManagedBean
@RequestScoped
public class CopyBean {
    private final Logger log = LoggerFactory.getLogger(CopyBean.class);

    private String currentProjectName;
    private String newProjectName;
    private String comment;
    private Boolean copyOldRevisions;
    private Integer revisionsCount;

    public boolean getCanCreate() {
        return isGranted(CREATE_PROJECTS);
    }

    public String getCurrentProjectName() {
        if (currentProjectName == null) {
            RulesProject project = WebStudioUtils.getWebStudio().getCurrentProject();
            return project == null ? null : project.getName();
        }
        return currentProjectName;
    }

    public void setCurrentProjectName(String currentProjectName) {
        this.currentProjectName = currentProjectName;
    }

    public String getNewProjectName() {
        return newProjectName;
    }

    public void setNewProjectName(String newProjectName) {
        this.newProjectName = StringUtils.trimToNull(newProjectName);
    }

    public String getComment() {
        if (comment == null) {
            return Constants.COPIED_FROM_PREFIX + " " + getCurrentProjectName();
        }
        return comment;
    }

    public void setComment(String comment) {
        this.comment = StringUtils.trimToNull(comment);
    }

    public void setCopyOldRevisions(Boolean copyOldRevisions) {
        this.copyOldRevisions = copyOldRevisions;
    }

    public Boolean getCopyOldRevisions() {
        return copyOldRevisions;
    }

    public void setRevisionsCount(Integer revisionsCount) {
        this.revisionsCount = revisionsCount;
    }

    public Integer getRevisionsCount() {
        if (revisionsCount == null) {
            return getMaxRevisionsCount();
        }
        return revisionsCount;
    }

    public int getMaxRevisionsCount() {
        RulesProject project = WebStudioUtils.getWebStudio().getCurrentProject();
        return project == null ? 0 : project.getVersionsCount() - project.getFirstRevisionIndex();
    }

    public void copy() {
        try {
            RulesUserSession rulesUserSession = WebStudioUtils.getRulesUserSession(FacesUtils.getSession());
            UserWorkspace userWorkspace = rulesUserSession.getUserWorkspace();
            DesignTimeRepository designTimeRepository = userWorkspace.getDesignTimeRepository();

            Repository designRepository = designTimeRepository.getRepository();
            LocalRepository localRepository = userWorkspace.getLocalWorkspace().getRepository();

            RulesProject project = userWorkspace.getProject(currentProjectName, false);
            String designPath = designTimeRepository.createProject(newProjectName).getFolderPath();

            if (copyOldRevisions) {
                List<ProjectVersion> versions = project.getVersions();
                int start = versions.size() - revisionsCount;
                for (int i = start; i < versions.size(); i++) {
                    ProjectVersion version = versions.get(i);
                    FileData fileData = new FileData();
                    fileData.setName(designPath);
                    fileData.setAuthor(version.getVersionInfo().getCreatedBy());
                    fileData.setComment(version.getVersionComment());
                    designRepository.copyHistory(project.getDesignFolderName(), fileData, version.getRevision());
                }
            }

            AProject designProject = new AProject(designRepository, designPath, false);
            AProject localProject = new AProject(localRepository, project.getFolderPath(), true);
            designProject.getFileData().setComment(comment);
            designProject.setResourceTransformer(new ProjectDescriptorTransformer(newProjectName));
            designProject.update(localProject, userWorkspace.getUser());
            designProject.setResourceTransformer(null);

            RulesProject copiedProject = new RulesProject(userWorkspace,
                    localRepository,
                    null,
                    designRepository,
                    designProject.getFileData(),
                    userWorkspace.getProjectsLockEngine());
            copiedProject.open();

            WebStudioUtils.getWebStudio().resetProjects();
            userWorkspace.refresh();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ValidatorException(new FacesMessage("Can't copy the project: " + e.getMessage()));
        }
    }

    public void newProjectNameValidator(FacesContext context, UIComponent toValidate, Object value) {
        String newProjectName = StringUtils.trim((String) value);
        if (StringUtils.isBlank(newProjectName)) {
            throw new ValidatorException(new FacesMessage("Can't be empty."));
        }
        RulesUserSession rulesUserSession = WebStudioUtils.getRulesUserSession(FacesUtils.getSession());
        try {
            UserWorkspace userWorkspace = rulesUserSession.getUserWorkspace();
            if (userWorkspace.hasProject(newProjectName)) {
                throw new ValidatorException(new FacesMessage("Project " + newProjectName + " exists already."));
            }
            
        } catch (WorkspaceException e) {
            log.error(e.getMessage(), e);
            throw new ValidatorException(new FacesMessage("Error during validation"));
        }
    }
}
