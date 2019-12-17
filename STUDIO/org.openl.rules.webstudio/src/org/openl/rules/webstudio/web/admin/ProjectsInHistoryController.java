package org.openl.rules.webstudio.web.admin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.config.ConfigurationManager;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;

@ManagedBean(name = "projectsInHistory")
@RequestScoped
public class ProjectsInHistoryController {

    @ManagedProperty(value = "#{environment}")
    private PropertyResolver propertyResolver;

    public static class ProjectBean {
        private String projectName;

        private boolean selected;

        public ProjectBean() {
        }

        public ProjectBean(String projectName) {
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

    private final Logger log = LoggerFactory.getLogger(ProjectsInHistoryController.class);

    private static final String PROJECT_HISTORY_HOME = "project.history.home";

    private ConfigurationManager configManager = WebStudioUtils.getWebStudio(true).getSystemConfigManager();
    private List<ProjectBean> projectBeans;

    public List<ProjectBean> getProjects() {
        if (projectBeans == null) {
            projectBeans = new ArrayList<>();
            File[] projects = new File(getProjectHistoryHome()).listFiles();
            if (projects != null) {
                for (File f : projects) {
                    projectBeans.add(new ProjectBean(f.getName()));
                }
            }
        }
        return projectBeans;
    }

    public String deleteProjects() {
        List<ProjectBean> beans = projectBeans;
        projectBeans = null;
        String msg;
        if (beans != null) {
            StringBuilder beansNamesBuilder = new StringBuilder();
            int beansSize = 0;
            for (ProjectBean bean : beans) {
                if (bean.isSelected()) {
                    try {
                        String projectPath = getProjectHistoryHome() + File.separator + bean.getProjectName();
                        FileUtils.delete(new File(projectPath));
                    } catch (Exception e) {
                        msg = "Failed to clean history of project '" + bean.getProjectName() + "'.";
                        log.error(msg, e);
                        FacesUtils.addErrorMessage(msg, e.getMessage());
                    }
                    beansNamesBuilder.append(bean.getProjectName()).append(", ");
                    beansSize = beansSize + 1;
                }
            }
            String beansNames = beansNamesBuilder.toString();
            if (beansSize != 0) {
                beansNames = beansNames.substring(0, beansNames.length() - 2);
                if (beansSize == 1) {
                    msg = "The history of " + beansNames + " was cleaned successfully";
                } else {
                    msg = "Histories of projects " + beansNames + " were cleaned successfully";
                }
                FacesUtils.addInfoMessage(msg);
            }
        }
        return null;
    }

    public String getProjectHistoryHome() {
        return propertyResolver.getProperty(PROJECT_HISTORY_HOME);
    }

    public void setPropertyResolver(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }
}
