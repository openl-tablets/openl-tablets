package org.openl.rules.webstudio.web.repository;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.jsf.annotation.ViewScope;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service("localUpload")
@ViewScope
public class LocalUploadController {
    public static class UploadBean {
        private final String projectName;

        private boolean selected;

        UploadBean(String projectName) {
            this.projectName = projectName;
        }

        public String getProjectName() {
            return projectName;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }

    private final Logger log = LoggerFactory.getLogger(LocalUploadController.class);

    private List<UploadBean> uploadBeans;

    private String repositoryId;

    private String projectFolder = "";

    private String createProjectCommentTemplate;

    private final Comments designRepoComments;

    public LocalUploadController(@Qualifier("designRepositoryComments") Comments designRepoComments) {
        this.designRepoComments = designRepoComments;
    }

    private void createProject(File baseFolder,
        RulesUserSession rulesUserSession,
        String comment,
        String repositoryId) throws ProjectException, WorkspaceException, FileNotFoundException {
        if (!baseFolder.isDirectory()) {
            throw new FileNotFoundException(baseFolder.getName());
        }

        rulesUserSession.getUserWorkspace().uploadLocalProject(repositoryId, baseFolder.getName(), projectFolder, comment);
    }

    public List<UploadBean> getProjects4Upload() {
        if (uploadBeans == null) {
            uploadBeans = new ArrayList<>();
            RulesUserSession userRules = WebStudioUtils.getRulesUserSession();
            WebStudio webStudio = WebStudioUtils.getWebStudio();
            if (webStudio != null && userRules != null) {
                UserWorkspace userWorkspace;
                DesignTimeRepository dtr;
                try {
                    userWorkspace = userRules.getUserWorkspace();
                    dtr = userWorkspace.getDesignTimeRepository();
                } catch (Exception e) {
                    log.error("Cannot get DTR.", e);
                    return null;
                }
                ProjectResolver projectResolver = webStudio.getProjectResolver();

                LocalWorkspace localWorkspace = userWorkspace.getLocalWorkspace();
                List<AProject> localProjects = new ArrayList<>(localWorkspace.getProjects());
                localProjects.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));
                for (AProject project : localProjects) {
                    try {
                        String repoId = project.getRepository().getId();
                        File projectFolder = new File(localWorkspace.getLocation(), project.getFolderPath());
                        ResolvingStrategy strategy = projectResolver.isRulesProject(projectFolder);
                        if (strategy != null && !dtr.hasProject(repoId, project.getName())) {
                            uploadBeans.add(new UploadBean(project.getName()));
                        }
                    } catch (Exception e) {
                        log.error("Failed to list projects for upload.", e);
                        WebStudioUtils.addErrorMessage(e.getMessage());
                    }
                }
            }
        }
        return uploadBeans;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
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

    private static final Comparator<File> fileNameComparator = (f1, f2) -> {
        String name1 = f1.getName();
        String name2 = f2.getName();
        return name1.compareToIgnoreCase(name2);
    };

    public String upload() {
        if (StringUtils.isBlank(repositoryId)) {
            WebStudioUtils.addErrorMessage("Repository must be selected.");
            return null;
        }

        String workspacePath = WebStudioUtils.getWebStudio().getWorkspacePath();
        RulesUserSession rulesUserSession = WebStudioUtils.getRulesUserSession();

        List<UploadBean> beans = uploadBeans;
        uploadBeans = null; // force re-read.

        if (beans != null) {
            for (UploadBean bean : beans) {
                if (bean.isSelected()) {
                    try {
                        String comment = designRepoComments.createProject(createProjectCommentTemplate,
                            bean.getProjectName());

                        createProject(new File(workspacePath, bean.getProjectName()), rulesUserSession, comment,
                            repositoryId);
                        WebStudioUtils.addInfoMessage("Project " + bean.getProjectName() + " was created successfully");
                    } catch (Exception e) {
                        String msg;
                        if (!NameChecker.checkName(bean.getProjectName())) {
                            msg = "Failed to create the project '" + bean
                                .getProjectName() + "'! " + NameChecker.BAD_PROJECT_NAME_MSG;
                        } else if (e.getCause() instanceof FileNotFoundException) {
                            if (e.getMessage().contains(".xls")) {
                                msg = "Failed to create the project. Please close module Excel file and try again.";
                            } else {
                                msg = "Failed to create the project because some resources are used";
                            }
                        } else {
                            msg = "Failed to create the project '" + bean.getProjectName() + "'.";
                            log.error(msg, e);
                        }
                        WebStudioUtils.addErrorMessage(msg);

                    }
                }
            }
        }

        return null;
    }

    public String getCreateProjectCommentTemplate() {
        if (createProjectCommentTemplate == null) {
            return designRepoComments.getCreateProjectTemplate();
        }
        return createProjectCommentTemplate;
    }

    public void setCreateProjectCommentTemplate(String createProjectComment) {
        this.createProjectCommentTemplate = createProjectComment;
    }

    public boolean isSelectAll() {
        return false;
    }

    public void setSelectAll(boolean selectAll) {
    }

    public boolean isSupportsMappedFolders() {
        if (StringUtils.isBlank(repositoryId)) {
            return false;
        }

        try {
            UserWorkspace userWorkspace = WebStudioUtils.getRulesUserSession().getUserWorkspace();
            return userWorkspace.getDesignTimeRepository().getRepository(repositoryId).supports().mappedFolders();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }
}
