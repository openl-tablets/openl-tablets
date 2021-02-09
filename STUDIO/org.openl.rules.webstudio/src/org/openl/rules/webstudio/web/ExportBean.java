package org.openl.rules.webstudio.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.util.Dates;
import org.openl.rules.webstudio.util.ExportFile;
import org.openl.rules.webstudio.web.repository.RepositoryUtils;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.Utils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.impl.ProjectExportHelper;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

@Service
@SessionScope
public class ExportBean {
    private static final Logger LOG = LoggerFactory.getLogger(ExportBean.class);

    private static final String LOCAL_VERSION = "Local version";

    private String repositoryId;
    private String currentProjectName;
    private String version;

    @Autowired
    private Utils utils;

    private static UserWorkspace getUserWorkspace() throws WorkspaceException {
        RulesUserSession rulesUserSession = WebStudioUtils.getRulesUserSession();
        return rulesUserSession.getUserWorkspace();
    }

    public void exportProject() {
        String cookePrefix = Constants.RESPONSE_MONITOR_COOKIE;
        String cookieName = cookePrefix + "_" + WebStudioUtils.getRequestParameter(cookePrefix);
        File file = null;
        try {
            String fileName = null;
            UserWorkspace userWorkspace = getUserWorkspace();
            RulesProject selectedProject = userWorkspace.getProject(repositoryId, currentProjectName, false);
            if (version == null || version.equals(LOCAL_VERSION)) {
                selectedProject.refresh();
                String userName = WebStudioUtils.getRulesUserSession().getUserName();

                FileData fileData = selectedProject.getFileData();
                String modifiedOnStr = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(fileData.getModifiedAt());
                String suffix = fileData.getAuthor() + "-" + modifiedOnStr;
                fileName = String.format("%s-%s.zip", selectedProject.getName(), suffix);
                file = ProjectExportHelper.export(new WorkspaceUserImpl(userName), selectedProject);
            } else {
                Repository repository = selectedProject.getDesignRepository();
                String branch = repository.supports().branches() ? ((BranchRepository) repository).getBranch() : null;
                AProject forExport = userWorkspace.getDesignTimeRepository()
                        .getProjectByPath(repository.getId(), branch, selectedProject.getRealPath(), version);
                file = ProjectExportHelper.export(userWorkspace.getUser(), forExport);
                String suffix = RepositoryUtils.buildProjectVersion(forExport.getFileData());
                fileName = String.format("%s-%s.zip", selectedProject.getBusinessName(), suffix);
            }
            addCookie(cookieName, "success", -1);
            final FacesContext facesContext = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) WebStudioUtils.getExternalContext().getResponse();
            ExportFile.writeOutContent(response, file, fileName);
            facesContext.responseComplete();
        } catch (Exception e) {
            String message;
            if (e.getCause() instanceof FileNotFoundException) {
                if (e.getMessage().contains(".xls")) {
                    message = "Failed to export the project. Please close module Excel file and try again.";
                } else {
                    message = "Failed to export the project because some resources are used.";
                }
            } else {
                message = "Failed to export the project. See logs for details.";
            }
            LOG.error(message, e);
            addCookie(cookieName, message, -1);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }


    public List<SelectItem> getSelectedProjectVersions() {
        List<SelectItem> projectVersions = new ArrayList<>();
        try {
            UserWorkspace userWorkspace = getUserWorkspace();
            if (repositoryId != null && currentProjectName != null) {
                RulesProject project = userWorkspace.getProject(repositoryId, currentProjectName, false);
                if (project.isOpened()) {
                    projectVersions.add(new SelectItem(LOCAL_VERSION, LOCAL_VERSION));
                }
                List<ProjectVersion> versions = project.getVersions();
                Collections.reverse(versions);
                projectVersions.addAll(toSelectItems(versions));
            }
        } catch (WorkspaceException | ProjectException e) {
            LOG.error(e.getMessage(), e);
        }
        return projectVersions;
    }

    public List<SelectItem> toSelectItems(Collection<ProjectVersion> versions) {
        if (versions == null) {
            return new ArrayList<>();
        }
        List<SelectItem> selectItems = new ArrayList<>();
        for (ProjectVersion version : versions) {
            if (!version.isDeleted()) {
                selectItems.add(new SelectItem(version.getVersionName(), utils.getDescriptiveVersion(version)));
            }
        }
        return selectItems;
    }

    private static void addCookie(String name, String value, int age) {
        Cookie cookie = new Cookie(name, StringTool.encodeURL(value));
        String contextPath = ((HttpServletRequest) WebStudioUtils.getExternalContext().getRequest()).getContextPath();
        if (!StringUtils.isEmpty(contextPath)) {
            cookie.setPath(contextPath);
        } else {
            cookie.setPath("/"); // EPBDS-7613
        }
        cookie.setMaxAge(age);
        ((HttpServletResponse) WebStudioUtils.getExternalContext().getResponse()).addCookie(cookie);
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

    public void setInitProject(String currentProjectName) {
        this.currentProjectName = currentProjectName;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getCurrentProjectName() {
        return currentProjectName;
    }

    public void setCurrentProjectName(String currentProjectName) {
        this.currentProjectName = currentProjectName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
