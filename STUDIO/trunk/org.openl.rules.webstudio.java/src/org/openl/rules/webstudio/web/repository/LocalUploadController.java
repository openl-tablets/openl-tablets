package org.openl.rules.webstudio.web.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.dtr.DesignTimeRepository;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class LocalUploadController {
    public static class UploadBean {
        private String projectName;

        private boolean selected;

        public UploadBean() {
        }
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

    private static final Log LOG = LogFactory.getLog(LocalUploadController.class);

    private List<UploadBean> uploadBeans;

    private void createProject(File baseFolder, RulesUserSession rulesUserSession) throws ProjectException,
            WorkspaceException, FileNotFoundException {
        if (!baseFolder.isDirectory()) {
            throw new FileNotFoundException(baseFolder.getName());
        }

        rulesUserSession.getUserWorkspace().uploadLocalProject(baseFolder.getName());

    }

    public List<UploadBean> getProjects4Upload() {
        if (uploadBeans == null) {
            uploadBeans = new ArrayList<UploadBean>();
            RulesUserSession userRules = getRules();
            WebStudio webStudio = WebStudioUtils.getWebStudio();
            if (webStudio != null && userRules != null) {
                DesignTimeRepository dtr;
                try {
                    dtr = userRules.getUserWorkspace().getDesignTimeRepository();
                } catch (Exception e) {
                    LOG.error("Cannot get DTR!", e);
                    return null;
                }

                List<File> projects = webStudio.getProjectResolver().listOpenLFolders();
                for (File f : projects) {
                    try {
                        if (!dtr.hasProject(f.getName())) {
                            uploadBeans.add(new UploadBean(f.getName()));
                        }
                    } catch (Exception e) {
                        LOG.error("Failed to list projects for upload!", e);
                        FacesUtils.addErrorMessage(e.getMessage());
                    }
                }
            }
        }
        return uploadBeans;
    }

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
                        createProject(new File(workspacePath, bean.getProjectName()), rulesUserSession);
                        FacesUtils.addInfoMessage("Project " + bean.getProjectName()
                                        + " was uploaded succesfully");
                    } catch (Exception e) {
                        String msg = "Failed to upload local project '" + bean.getProjectName() + "'!";
                        LOG.error(msg, e);
                        FacesUtils.addErrorMessage(msg, e.getMessage());
                    }
                }
            }
        }

        return null;
    }
}
