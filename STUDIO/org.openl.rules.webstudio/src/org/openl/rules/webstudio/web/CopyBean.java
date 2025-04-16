package org.openl.rules.webstudio.web;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.jsf.FacesContextUtils;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.ui.Message;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.admin.FolderStructureValidators;
import org.openl.rules.webstudio.web.admin.ProjectTagsBean;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.admin.RepositorySettingsValidators;
import org.openl.rules.webstudio.web.repository.CommentValidator;
import org.openl.rules.webstudio.web.repository.RepositoryTreeState;
import org.openl.rules.webstudio.web.repository.tree.TreeProject;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.ProjectArtifactUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.impl.FileMappingData;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.permission.AclPermissionsSets;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.util.StringUtils;

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

    private final ProjectTagsBean projectTagsBean;

    private final RepositoryAclService designRepositoryAclService;

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

    public CopyBean(PropertyResolver propertyResolver,
                    RepositoryTreeState repositoryTreeState,
                    ProjectTagsBean projectTagsBean,
                    @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService) {
        this.propertyResolver = propertyResolver;
        this.repositoryTreeState = repositoryTreeState;
        this.projectTagsBean = projectTagsBean;
        this.designRepositoryAclService = designRepositoryAclService;
    }

    public boolean getCanCopy(AProject project) {
        if (getCanCreateNewProject()) {
            return true;
        }
        return getCanCopyToNewBranch(project);
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
        } catch (ProjectException e) {
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
        this.projectFolder = prepareProjectFolder(projectFolder);
    }

    private static String prepareProjectFolder(String projectFolder) {
        String folder = StringUtils.trimToEmpty(projectFolder).replace('\\', '/');
        if (folder.startsWith("/")) {
            folder = folder.substring(1);
        }
        if (!folder.isEmpty() && !folder.endsWith("/")) {
            folder += '/';
        }
        return folder;
    }

    public boolean isSeparateProject() {
        boolean canCreateNewProject = getCanCreateNewProject();
        boolean canCopyToNewBranch = getCanCopyToNewBranch(getCurrentProject());
        if (canCopyToNewBranch && canCreateNewProject) {
            return separateProject;
        }
        if (canCopyToNewBranch) {
            return false;
        }
        return canCreateNewProject;
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
        if (StringUtils.isEmpty(currentProjectName)) {
            errorMessage = "Project is not selected.";
            return;
        }
        try {
            errorMessage = null;

            UserWorkspace userWorkspace = getUserWorkspace();
            DesignTimeRepository designTimeRepository = userWorkspace.getDesignTimeRepository();

            RulesProject project = userWorkspace.getProject(repositoryId, currentProjectName, false);
            if (!designRepositoryAclService.isGranted(project, List.of(AclPermission.VIEW))) {
                throw new Message("There is no permission for copying the project.");
            }
            if (isSupportsBranches() && !isSeparateProject()) {
                Repository designRepository = project.getDesignRepository();
                ((BranchRepository) designRepository).createBranch(project.getDesignFolderName(), newBranchName);
            } else {
                Repository designRepository = designTimeRepository.getRepository(toRepositoryId);
                if (!designRepositoryAclService.isGranted(toRepositoryId, null, List.of(AclPermission.CREATE))) {
                    throw new Message("There is no permission for creating the project.");
                }
                String designPath = designTimeRepository.getRulesLocation() + newProjectName;
                FileData designData = new FileData();
                designData.setName(designPath);

                FileMappingData mappingData = new FileMappingData(designPath, projectFolder + newProjectName);
                WorkspaceUser user = userWorkspace.getUser();

                if (copyOldRevisions && repositoryId.equals(toRepositoryId)) {
                    List<ProjectVersion> versions = project.getVersions();
                    int start = versions.size() - revisionsCount;
                    for (int i = start; i < versions.size(); i++) {
                        ProjectVersion version = versions.get(i);
                        String createdBy = version.getVersionInfo().getCreatedBy();
                        FileData fileData = new FileData();
                        fileData.setName(designPath);
                        fileData.setAuthor(
                                new UserInfo(createdBy, version.getVersionInfo().getEmailCreatedBy(), createdBy));
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
                Map<String, String> tags = projectTagsBean.saveTagsTypesAndGetTags();
                designProject.setResourceTransformer(new CopyProjectTransformer(newProjectName, tags));
                
                designProject.update(localProject, user);
                designProject.setResourceTransformer(null);

                RulesProject copiedProject = new RulesProject(user,
                        userWorkspace.getLocalWorkspace().getRepository(toRepositoryId),
                        null,
                        designRepository,
                        designProject.getFileData(),
                        userWorkspace.getProjectsLockEngine());
                if (!designRepositoryAclService
                        .createAcl(copiedProject, AclPermissionsSets.NEW_PROJECT_PERMISSIONS, true)) {
                    String message = String.format("Granting permissions to a new project '%s' is failed.",
                            ProjectArtifactUtils.extractResourceName(copiedProject));
                    WebStudioUtils.addErrorMessage(message);
                }

                if (!userWorkspace.isOpenedOtherProject(copiedProject)) {
                    copiedProject.open();
                }
            }

            WebStudioUtils.getWebStudio().resetProjects();
            userWorkspace.refresh();
            repositoryTreeState.invalidateTree();

            switchToNewBranch();
            currentProjectName = null;
            WebStudioUtils.addInfoMessage("Project copied successfully.");
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            errorMessage = "Cannot copy the project: " + e.getMessage();
        }
    }

    private void switchToNewBranch() {
        if (!(isSupportsBranches() && !isSeparateProject())) {
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
            if (selectedProject == null) {
                return;
            }
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
        if (isSupportsBranches() && !isSeparateProjectSubmitted(context) || ((String) value).isEmpty()) {
            return;
        }

        String newProjectName = StringUtils.trim((String) value);
        WebStudioUtils.validate(NameChecker.checkName(newProjectName), NameChecker.BAD_PROJECT_NAME_MSG);

        RulesUserSession rulesUserSession = WebStudioUtils.getRulesUserSession();
        UserWorkspace userWorkspace = rulesUserSession.getUserWorkspace();

        String targetRepo = ((UIInput) context.getViewRoot().findComponent("copyProjectForm:repository"))
                .getSubmittedValue()
                .toString();
        boolean projectExists = userWorkspace.getDesignTimeRepository().hasProject(targetRepo, newProjectName);
        WebStudioUtils.validate(!projectExists, "Project with this name already exists.");
    }

    public void newBranchNameValidator(FacesContext context, UIComponent toValidate, Object value) {
        if (!isSupportsBranches() || isSeparateProjectSubmitted(context)) {
            return;
        }
        String newBranchName = (String) value;
        WebStudioUtils.validate(StringUtils.isNotBlank(newBranchName), "Branch name cannot be empty.");
        RepositorySettingsValidators.validateBranchName(newBranchName);

        String customRegex = propertyResolver
                .getProperty(Comments.REPOSITORY_PREFIX + repositoryId + ".new-branch.regex");
        String customRegexError = propertyResolver
                .getProperty(Comments.REPOSITORY_PREFIX + repositoryId + ".new-branch.regex-error");
        if (StringUtils.isNotBlank(customRegex)) {
            try {
                Pattern customRegexPattern = Pattern.compile(customRegex);
                customRegexError = StringUtils
                        .isNotBlank(customRegexError) ? customRegexError
                        : "Branch name must match the following pattern: " + customRegex;
                WebStudioUtils.validate(customRegexPattern.matcher(newBranchName).matches(), customRegexError);
            } catch (PatternSyntaxException patternSyntaxException) {
                LOG.debug(patternSyntaxException.getMessage(), patternSyntaxException);
                WebStudioUtils.throwValidationError("Invalid regex pattern for branch name.");
            }
        }

        try {
            UserWorkspace userWorkspace = getUserWorkspace();
            DesignTimeRepository designTimeRepository = userWorkspace.getDesignTimeRepository();

            BranchRepository designRepository = (BranchRepository) designTimeRepository.getRepository(repositoryId);
            WebStudioUtils.validate(designRepository.isValidBranchName(newBranchName),
                    "Branch name contains reserved words or symbols.");
            WebStudioUtils.validate(!designRepository.branchExists(newBranchName),
                    "Branch " + newBranchName + " already exists in repository.");
            for (String branch : designRepository.getBranches(null)) {
                String message = "Cannot create the branch '" + newBranchName + "' because the branch '" + branch + "' already exists.\n" + "Explanation: for example a branch 'foo/bar'exists. " + "That branch can be considered as a file 'bar' located in the folder 'foo'.\n" + "So you cannot create a branch 'foo/bar/baz' because you cannot create the folder 'foo/bar': " + "the file with such name already exists.";
                WebStudioUtils.validate(!newBranchName.startsWith(branch + "/"), message);
            }
        } catch (IOException e) {
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

    public void projectPathValidator(FacesContext context, UIComponent toValidate, Object value) {
        final String projPath = prepareProjectFolder((String) value);
        final String projName = StringUtils
                .trimToEmpty(WebStudioUtils.getRequestParameter("copyProjectForm:newProjectName"));
        FolderStructureValidators.validatePathInRepository(projPath);
        final Path currentPath = Paths.get(StringUtils.isEmpty(projPath) ? projName : projPath + projName);
        UserWorkspace userWorkspace = getUserWorkspace();
        if (userWorkspace.getDesignTimeRepository()
                .getProjects()
                .stream()
                .filter(proj -> proj.getRepository().getId().equals(repositoryId))
                .map(AProjectFolder::getRealPath)
                .map(Paths::get)
                .anyMatch(path -> path.startsWith(currentPath) || currentPath.startsWith(path))) {
            WebStudioUtils.throwValidationError("Path conflicts with an existing project.");
        }
    }

    private static Boolean isSeparateProjectSubmitted(FacesContext context) {
        return (Boolean) ((UIInput) context.getViewRoot().findComponent("copyProjectForm:separateProjectCheckbox"))
                .getValue();
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
        this.toRepositoryId = repositoryId;
        UserWorkspace userWorkspace = getUserWorkspace();
        DesignTimeRepository designTimeRepository = userWorkspace.getDesignTimeRepository();
        if (designTimeRepository.getRepository(toRepositoryId) == null) {
            toRepositoryId = designTimeRepository.getRepositories().get(0).getId();
        }

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
                String userName = getUserWorkspace().getUser().getUserName();
                String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
                newBranchName = designRepoComments.newBranch(getBusinessName(), userName, date);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public boolean isSupportsBranches() {
        RulesProject project = getCurrentProject();
        return project != null && project.isSupportsBranches();
    }

    public List<Repository> getAllowedRepositories() {
        DesignTimeRepository designRepo = getUserWorkspace().getDesignTimeRepository();
        return designRepo.getRepositories()
                .stream()
                .filter(repo -> !repo.supports().branches() || !((BranchRepository) repo)
                        .isBranchProtected(((BranchRepository) repo).getBranch()))
                .collect(Collectors.toList());
    }

    public String getDestRepositoryType() {
        return Optional.ofNullable(toRepositoryId)
                .map(repoId -> new RepositoryConfiguration(repoId, propertyResolver))
                .map(RepositoryConfiguration::getType)
                .orElse(null);
    }

    public boolean getCanCreateNewProject() {
        if (getAllowedRepositories().isEmpty()) {
            return false;
        }
        UserWorkspace userWorkspace = WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());
        for (Repository repository : userWorkspace.getDesignTimeRepository().getRepositories()) {
            if (designRepositoryAclService.isGranted(repository.getId(), null, List.of(AclPermission.CREATE))) {
                return true;
            }
        }
        return false;
    }

    public boolean getCanCopyToNewBranch(AProject project) {
        boolean branchesSupported = project.getRepository().supports().branches();
        if (project instanceof RulesProject) {
            RulesProject rulesProject = (RulesProject) project;
            branchesSupported = rulesProject.isSupportsBranches();
        }
        if (branchesSupported) {
            for (AProjectArtefact artefact : project.getArtefacts()) {
                if (designRepositoryAclService.isGranted(artefact,
                        List.of(AclPermission.EDIT, AclPermission.DELETE, AclPermission.ADD))) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isSupportsMappedFolders() {
        try {
            if (toRepositoryId == null) {
                return false;
            }

            UserWorkspace userWorkspace = getUserWorkspace();
            DesignTimeRepository designTimeRepository = userWorkspace.getDesignTimeRepository();

            Repository designRepository = designTimeRepository.getRepository(toRepositoryId);
            if (designRepository == null) {
                return false;
            }
            return designRepository.supports().mappedFolders();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return false;
        }
    }

    public RulesProject getCurrentProject() {
        if (StringUtils.isBlank(currentProjectName)) {
            return null;
        }

        try {
            UserWorkspace userWorkspace = getUserWorkspace();
            RulesProject rulesProject = userWorkspace.getProject(repositoryId, currentProjectName, false);
            if (rulesProject == null) {
                currentProjectName = null;
            }
            return rulesProject;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    private static UserWorkspace getUserWorkspace() {
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
