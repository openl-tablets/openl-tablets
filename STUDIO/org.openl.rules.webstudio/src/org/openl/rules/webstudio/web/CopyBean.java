package org.openl.rules.webstudio.web;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.ProjectHistoryService;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.CommentValidator;
import org.openl.rules.webstudio.web.repository.RepositoryTreeState;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.jsf.FacesContextUtils;

import static org.openl.rules.security.AccessManager.isGranted;
import static org.openl.rules.security.Privileges.CREATE_PROJECTS;

/**
 * FIXME: Replace SessionScoped with RequestScoped when validation issues in inputNumberSpinner in Repository and Editor
 * tabs will be fixed.
 */
@Service
@SessionScope
public class CopyBean {
    private static final Logger LOG = LoggerFactory.getLogger(CopyBean.class);

    private final PropertyResolver propertyResolver;

    private final RepositoryTreeState repositoryTreeState;

    private final ApplicationContext applicationContext = FacesContextUtils
            .getRequiredWebApplicationContext(FacesContext.getCurrentInstance());

    private String repositoryId;
    private String toRepositoryId;
    private boolean repositoryIsChanged = false;

    private String currentProjectName;
    private String newProjectName;
    private String projectFolder;
    private boolean separateProject;
    private String currentBranchName;
    private String newBranchName;
    private String comment;
    private Boolean copyOldRevisions = Boolean.FALSE;
    private Integer revisionsCount;
    private String errorMessage;
    private Comments designRepoComments;

    public CopyBean(PropertyResolver propertyResolver, RepositoryTreeState repositoryTreeState) {
        this.propertyResolver = propertyResolver;
        this.repositoryTreeState = repositoryTreeState;
    }

    public boolean getCanCreate() {
        return isGranted(CREATE_PROJECTS);
    }

    public String getCurrentProjectName() {
        return currentProjectName;
    }

    public String getCurrentBranchName() {
        return currentBranchName;
    }

    public String getBusinessName() {
        if (repositoryId == null || currentProjectName == null) {
            return currentProjectName;
        }
        try {
            return getUserWorkspace().getProject(repositoryId, currentProjectName, false).getBusinessName();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

        return currentProjectName;
    }

    public String getNewProjectName() {
        return newProjectName;
    }

    public void setNewProjectName(String newProjectName) {
        this.newProjectName = StringUtils.trimToNull(newProjectName);
    }

    public String getProjectFolder() {
        String folderToShow = this.projectFolder;
        if (!folderToShow.startsWith("/")) {
            folderToShow = "/" + folderToShow;
        }
        return folderToShow;
    }

    public void setProjectFolder(String projectFolder) {
        String folder = StringUtils.trimToEmpty(projectFolder).replace('\\', '/');
        if (folder.startsWith("/")) {
            folder = folder.substring(1);
        }
        if (!folder.isEmpty() && !folder.endsWith("/")) {
            folder += '/';
        }
        this.projectFolder = folder;
    }

    public boolean isSeparateProject() {
        return separateProject;
    }

    public void setSeparateProject(boolean separateProject) {
        this.separateProject = separateProject;
    }

    public String getNewBranchName() {
        return newBranchName;
    }

    public void setNewBranchName(String newBranchName) {
        this.newBranchName = newBranchName;
    }

    public String getComment() {
        if (comment == null && designRepoComments != null) {
            return designRepoComments.copiedFrom(getBusinessName());
        }
        return comment;
    }

    public void setComment(String comment) {
        if (repositoryIsChanged) {
            this.comment = null;
            repositoryIsChanged = false;
        } else {
            this.comment = StringUtils.trimToNull(comment);
        }
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
        if (repositoryId == null || !repositoryId.equals(toRepositoryId)) {
            // We don't support copy history when copying to another repository.
            return 0;
        }
        RulesProject project = getCurrentProject();
        return project == null ? 0 : project.getVersionsCount();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void copy() {
        try {
            errorMessage = null;

            UserWorkspace userWorkspace = getUserWorkspace();
            DesignTimeRepository designTimeRepository = userWorkspace.getDesignTimeRepository();

            RulesProject project = userWorkspace.getProject(repositoryId, currentProjectName, false);
            ProjectHistoryService.deleteHistory(project.getBusinessName());
            if (isSupportsBranches() && !separateProject) {
                Repository designRepository = project.getDesignRepository();
                ((BranchRepository) designRepository).createBranch(project.getDesignFolderName(), newBranchName);
            } else {
                Repository designRepository = designTimeRepository.getRepository(toRepositoryId);
                String designPath = designTimeRepository.getRulesLocation() + newProjectName;
                FileData designData = new FileData();
                designData.setName(designPath);

                FileMappingData mappingData = new FileMappingData(designPath, projectFolder + newProjectName);

                if (copyOldRevisions && repositoryId.equals(toRepositoryId)) {
                    List<ProjectVersion> versions = project.getVersions();
                    int start = versions.size() - revisionsCount;
                    for (int i = start; i < versions.size(); i++) {
                        ProjectVersion version = versions.get(i);

                        FileData fileData = new FileData();
                        fileData.setName(designPath);
                        fileData.setAuthor(version.getVersionInfo().getCreatedBy());
                        fileData.setComment(version.getVersionComment());
                        if (designRepository.supports().mappedFolders()) {
                            fileData.addAdditionalData(mappingData);
                        }
                        designRepository.copyHistory(project.getDesignFolderName(), fileData, version.getRevision());
                    }
                }

                AProject designProject = new AProject(designRepository, designData);
                AProject localProject = new AProject(project.getRepository(), project.getFolderPath());
                if (designRepository.supports().mappedFolders()) {
                    designData.addAdditionalData(mappingData);
                }
                designData.setComment(comment);
                designProject.setResourceTransformer(new ProjectDescriptorTransformer(newProjectName));
                designProject.update(localProject, userWorkspace.getUser());
                designProject.setResourceTransformer(null);

                RulesProject copiedProject = new RulesProject(userWorkspace.getUser(),
                        userWorkspace.getLocalWorkspace().getRepository(toRepositoryId),
                        null,
                        designRepository,
                        designProject.getFileData(),
                        userWorkspace.getProjectsLockEngine());
                if (!userWorkspace.isOpenedOtherProject(copiedProject)) {
                    copiedProject.open();
                }
            }

            WebStudioUtils.getWebStudio().resetProjects();
            userWorkspace.refresh();
            repositoryTreeState.invalidateTree();

            switchToNewBranch();
            currentProjectName = null;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            errorMessage = "Cannot copy the project: " + e.getMessage();
        }
    }

    private void switchToNewBranch() {
        if (!(isSupportsBranches() && !separateProject)) {
            return;
        }
        try {
            UserWorkspaceProject selectedProject = getCurrentProject();
            if (selectedProject == null) {
                return;
            }
            TreeProject node = repositoryTreeState.getProjectNodeByPhysicalName(selectedProject.getRepository().getId(),
                    selectedProject.getName());
            selectedProject = repositoryTreeState.getProject(node);
            WebStudio studio = WebStudioUtils.getWebStudio();

            if (selectedProject.isOpened()) {
                studio.getModel().clearModuleInfo();
                selectedProject.releaseMyLock();
            }
            selectedProject.setBranch(newBranchName);
            // Update files
            if (!getUserWorkspace().isOpenedOtherProject(selectedProject)) {
                selectedProject.open();
            }

            repositoryTreeState.refreshNode(node);
            studio.reset();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void newProjectNameValidator(FacesContext context, UIComponent toValidate, Object value) {
        if ((isSupportsBranches() && !isSeparateProjectSubmitted(context)) || ((String) value).isEmpty()) {
            return;
        }
        String newProjectName = StringUtils.trim((String) value);
        WebStudioUtils.validate(NameChecker.checkName(newProjectName), NameChecker.BAD_PROJECT_NAME_MSG);

        RulesUserSession rulesUserSession = WebStudioUtils.getRulesUserSession();
        try {
            UserWorkspace userWorkspace = rulesUserSession.getUserWorkspace();
            String targetRepo = ((UIInput) context.getViewRoot().findComponent("copyProjectForm:repository"))
                    .getSubmittedValue()
                    .toString();
            boolean projectExist = userWorkspace.getDesignTimeRepository().hasProject(targetRepo, newProjectName);
            WebStudioUtils.validate(!projectExist, "Project with such name already exists.");
        } catch (WorkspaceException e) {
            LOG.error(e.getMessage(), e);
            WebStudioUtils.throwValidationError("Error during validation.");
        }
    }

    public void newBranchNameValidator(FacesContext context, UIComponent toValidate, Object value) {
        if (!isSupportsBranches() || isSeparateProjectSubmitted(context)) {
            return;
        }

        String newBranchName = StringUtils.trim((String) value);
        WebStudioUtils.validate(StringUtils.isNotBlank(newBranchName), "Cannot be empty.");
        WebStudioUtils.validate(newBranchName.matches("[\\w\\-/]+"),
                "Invalid branch name. Only latin letters, numbers, '_', '-' and '/' are allowed.");

        try {
            UserWorkspace userWorkspace = getUserWorkspace();
            DesignTimeRepository designTimeRepository = userWorkspace.getDesignTimeRepository();

            BranchRepository designRepository = (BranchRepository) designTimeRepository.getRepository(repositoryId);
            WebStudioUtils.validate(designRepository.isValidBranchName(newBranchName),
                    "Invalid branch name. It should not contain reserved words or symbols.");
            WebStudioUtils.validate(!designRepository.branchExists(newBranchName),
                    "Branch " + newBranchName + " already exists.");
            for (String branch : designRepository.getBranches(null)) {
                String message = "Can't create the branch '" +
                        newBranchName + "' because the branch '" +
                        branch + "' already exists.\n" + "Explanation: for example a branch 'foo/bar'exists. " +
                        "That branch can be considered as a file 'bar' located in the folder 'foo'.\n" +
                        "So you can't create a branch 'foo/bar/baz' because you can't create the folder 'foo/bar': " +
                        "the file with such name already exists.";
                WebStudioUtils.validate(!newBranchName.startsWith(branch + "/"), message);
            }
        } catch (WorkspaceException | IOException e) {
            LOG.debug("Ignored error: ", e);
        }

    }

    public void commentValidator(FacesContext context, UIComponent toValidate, Object value) {
        if (isSupportsBranches() && !isSeparateProjectSubmitted(context)) {
            return;
        }

        String comment = (String) value;

        RulesProject project = getCurrentProject();
        if (project != null && toRepositoryId != null) {
            CommentValidator.forRepo(toRepositoryId).validate(comment);
        }
    }

    private static Boolean isSeparateProjectSubmitted(FacesContext context) {
        return (Boolean) ((UIInput) context.getViewRoot().findComponent("copyProjectForm:separateProjectCheckbox"))
                .getValue();
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
        this.toRepositoryId = repositoryId;
        this.designRepoComments = new Comments(propertyResolver, toRepositoryId);
    }

    public String getToRepositoryId() {
        return toRepositoryId;
    }

    public void setToRepositoryId(String toRepositoryId) {
        if (this.toRepositoryId == null || !this.toRepositoryId.equals(toRepositoryId)) {
            repositoryIsChanged = true;
        }
        this.toRepositoryId = toRepositoryId;
    }

    public void setInitProject(String currentProjectName) {
        try {
            this.currentProjectName = currentProjectName;
            newProjectName = null;
            projectFolder = "";
            comment = null;
            copyOldRevisions = Boolean.FALSE;
            revisionsCount = null;
            separateProject = false;
            errorMessage = null;
            if (isSupportsBranches()) {
                RulesProject project = getCurrentProject();
                currentBranchName = project.getBranch();
                // Remove restricted symbols
                String simplifiedProjectName = getBusinessName().replaceAll("[^\\w\\-]", "");
                String userName = getUserWorkspace().getUser().getUserName();
                String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
                String pattern = applicationContext.getEnvironment()
                        .getProperty("repository.design.new-branch-pattern");
                Objects.requireNonNull(pattern);
                newBranchName = designRepoComments.newBranch(simplifiedProjectName, userName, date);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public boolean isSupportsBranches() {
        RulesProject project = getCurrentProject();
        return project != null && project.isSupportsBranches();
    }

    public boolean isSupportsMappedFolders() {
        try {
            if (toRepositoryId == null) {
                return false;
            }

            UserWorkspace userWorkspace = getUserWorkspace();
            DesignTimeRepository designTimeRepository = userWorkspace.getDesignTimeRepository();

            Repository designRepository = designTimeRepository.getRepository(toRepositoryId);
            return designRepository.supports().mappedFolders();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
    }

    private RulesProject getCurrentProject() {
        if (StringUtils.isBlank(currentProjectName)) {
            return null;
        }

        try {
            UserWorkspace userWorkspace = getUserWorkspace();
            if (!userWorkspace.hasProject(repositoryId, currentProjectName)) {
                currentProjectName = null;
                return null;
            }

            return userWorkspace.getProject(repositoryId, currentProjectName, false);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    private static UserWorkspace getUserWorkspace() throws WorkspaceException {
        RulesUserSession rulesUserSession = WebStudioUtils.getRulesUserSession();
        return rulesUserSession.getUserWorkspace();
    }

    public boolean isConfirmationRequired() {
        if (!isSupportsBranches()) {
            return false;
        }
        RulesProject project = getCurrentProject();
        return project != null && project.isOpened() && project.getStatus() == ProjectStatus.EDITING;
    }
}
