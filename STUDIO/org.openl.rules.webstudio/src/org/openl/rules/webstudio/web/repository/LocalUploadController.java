package org.openl.rules.webstudio.web.repository;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.servlet.http.HttpSession;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@ManagedBean(name = "localUpload")
@RequestScoped
public class LocalUploadController {
    public static class UploadBean {
        private String projectName;

        private boolean selected;

        public UploadBean(String projectName) {
            this.projectName = projectName;
        }

        public String getProjectName() {
            return projectName;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }

    private final Logger log = LoggerFactory.getLogger(LocalUploadController.class);

    private List<UploadBean> uploadBeans;

    private String projectFolder = "";

    private String createProjectCommentTemplate;

    @ManagedProperty(value = "#{designRepositoryComments}")
    private Comments designRepoComments;

    private void createProject(File baseFolder, RulesUserSession rulesUserSession, String comment) throws ProjectException,
                                                                                                          WorkspaceException, FileNotFoundException {
        if (!baseFolder.isDirectory()) {
            throw new FileNotFoundException(baseFolder.getName());
        }

        rulesUserSession.getUserWorkspace().uploadLocalProject(baseFolder.getName(), projectFolder, comment);
    }

    public List<UploadBean> getProjects4Upload() {
        if (uploadBeans == null) {
            uploadBeans = new ArrayList<>();
            RulesUserSession userRules = getRules();
            WebStudio webStudio = WebStudioUtils.getWebStudio();
            if (webStudio != null && userRules != null) {
                DesignTimeRepository dtr;
                try {
                    dtr = userRules.getUserWorkspace().getDesignTimeRepository();
                } catch (Exception e) {
                    log.error("Cannot get DTR!", e);
                    return null;
                }
                File workspace = new File(webStudio.getWorkspacePath());
                File[] projects = workspace.listFiles();
                if (projects == null) {
                    projects = new File[0];
                }
                Arrays.sort(projects, fileNameComparator);
                // All OpenL projects folders in workspace
                ProjectResolver projectResolver = webStudio.getProjectResolver();
                for (File f : projects) {
                    try {
                        ResolvingStrategy strategy = projectResolver.isRulesProject(f);
                        if (strategy != null && !dtr.hasProject(f.getName())) {
                            uploadBeans.add(new UploadBean(f.getName()));
                        }
                    } catch (Exception e) {
                        log.error("Failed to list projects for upload!", e);
                        FacesUtils.addErrorMessage(e.getMessage());
                    }
                }
            }
        }
        return uploadBeans;
    }

    public String getProjectFolder() {
        return projectFolder;
    }

    public void setProjectFolder(String projectFolder) {
        String folder = StringUtils.trimToEmpty(projectFolder).replace('\\', '/');
        if (!folder.isEmpty() && !folder.endsWith("/")) {
            folder += '/';
        }
        this.projectFolder = folder;
    }

    /**
     * EPBDS-8384: JSF beans discovery doesn't work if the bean contains static field with lambda expression. Possibly need
     * to upgrade JSF version to fully support java 8. Until then use anonymous class instead.
     */
    private static Comparator<File> fileNameComparator = new Comparator<File>() {
        @Override public int compare(File f1, File f2) {
            String name1 = f1.getName();
            String name2 = f2.getName();
            return name1.compareToIgnoreCase(name2);
        }
    };

    private RulesUserSession getRules() {
        HttpSession session = FacesUtils.getSession();
        return WebStudioUtils.getRulesUserSession(session);
    }

    public String upload() {
        String workspacePath = WebStudioUtils.getWebStudio().getWorkspacePath();
        RulesUserSession rulesUserSession = getRules();

        List<UploadBean> beans = uploadBeans;
        uploadBeans = null; // force re-read.

        if (beans != null) {
            for (UploadBean bean : beans) {
                if (bean.isSelected()) {
                    try {
                        String comment = designRepoComments.createProject(createProjectCommentTemplate, bean.getProjectName());

                        createProject(new File(workspacePath, bean.getProjectName()), rulesUserSession, comment);
                        FacesUtils.addInfoMessage("Project " + bean.getProjectName()
                                + " was created successfully");
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
                            msg = "Failed to create the project '" + bean.getProjectName() + "'!";
                            log.error(msg, e);
                        }
                        FacesUtils.addErrorMessage(msg);

                    }
                }
            }
        }

        return null;
    }

    public void setDesignRepoComments(Comments designRepoComments) {
        this.designRepoComments = designRepoComments;
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
}
