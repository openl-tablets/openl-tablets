package org.openl.rules.webstudio.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.util.ExportFile;
import org.openl.rules.webstudio.web.repository.RepositoryUtils;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.Utils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.impl.ProjectExportHelper;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;

@Service
@SessionScope
public class ExportBean {
    private static final Logger LOG = LoggerFactory.getLogger(ExportBean.class);

    private static final String VIEWING_VERSION = "Viewing";
    private static final String IN_EDITING_VERSION = "In Editing";

    private String repositoryId;
    private String currentProjectName;
    private String version;
    private String artifactName;

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private Utils utils;

    private static UserWorkspace getUserWorkspace() {
        RulesUserSession rulesUserSession = WebStudioUtils.getRulesUserSession();
        return rulesUserSession.getUserWorkspace();
    }

    public void exportProject() {
        String cookePrefix = Constants.RESPONSE_MONITOR_COOKIE;
        String cookieName = cookePrefix + "_" + WebStudioUtils.getRequestParameter(cookePrefix);
        File file = null;
        try {
            String fileName;
            UserWorkspace userWorkspace = getUserWorkspace();
            RulesProject selectedProject = userWorkspace.getProject(repositoryId, currentProjectName, false);
            if (version == null || version.equals(VIEWING_VERSION) || version.equals(IN_EDITING_VERSION)) {
                selectedProject.refresh();
                String userName = WebStudioUtils.getRulesUserSession().getUserName();

                FileData fileData = selectedProject.getFileData();
                String name = Optional.ofNullable(fileData.getAuthor()).map(UserInfo::getName).orElse(null);
                String modifiedOnStr = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(fileData.getModifiedAt());
                String suffix = name + "-" + modifiedOnStr;
                fileName = String.format("%s-%s.zip", selectedProject.getName(), suffix);
                WorkspaceUserImpl user = new WorkspaceUserImpl(userName,
                        (username) -> Optional.ofNullable(userManagementService.getUser(username))
                                .map(usr -> new UserInfo(usr.getUsername(), usr.getEmail(), usr.getDisplayName()))
                                .orElse(null));
                file = ProjectExportHelper.export(user, selectedProject);
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
                    message = "Failed to save the changes. Close the module Excel file and try again.";
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

    public void exportFileVersion() {
        File file = null;
        String cookePrefix = Constants.RESPONSE_MONITOR_COOKIE;
        String cookieName = cookePrefix + "_" + WebStudioUtils.getRequestParameter(cookePrefix);
        try {
            UserWorkspace userWorkspace = getUserWorkspace();
            RulesProject selectedProject = userWorkspace.getProject(repositoryId, currentProjectName, false);

            AProjectResource projectResource;
            if (version == null || version.equals(VIEWING_VERSION) || version.equals(IN_EDITING_VERSION)) {
                AProjectArtefact artefact = selectedProject.getArtefact(getArtifactName());
                projectResource = (AProjectResource) artefact;
            } else {
                Repository repository = selectedProject.getDesignRepository();
                String branch = repository.supports().branches() ? ((BranchRepository) repository).getBranch() : null;
                AProject forExport = userWorkspace.getDesignTimeRepository()
                        .getProjectByPath(repository.getId(), branch, selectedProject.getRealPath(), version);
                projectResource = (AProjectResource) forExport.getArtefact(getArtifactName());
            }

            file = File.createTempFile("export-", "-file");
            IOUtils.copyAndClose(projectResource.getContent(), new FileOutputStream(file));
            addCookie(cookieName, "success", -1);
            final FacesContext facesContext = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) WebStudioUtils.getExternalContext().getResponse();
            ExportFile.writeOutContent(response, file, getFileName());
            facesContext.responseComplete();
        } catch (Exception e) {
            String msg = "Failed to export file version. ";
            LOG.error(msg, e);
            addCookie(cookieName, msg + e.getMessage(), -1);
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
                    if (project.isModified()) {
                        projectVersions.add(new SelectItem(IN_EDITING_VERSION, IN_EDITING_VERSION));
                    } else {
                        projectVersions.add(new SelectItem(VIEWING_VERSION, VIEWING_VERSION));
                    }
                }
                List<ProjectVersion> versions = project.getVersions();
                Collections.reverse(versions);
                projectVersions.addAll(toSelectItems(versions));
            }
        } catch (ProjectException e) {
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
        cookie.setHttpOnly(false); // Has to be visible from client scripting
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
        this.version = null;
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

    public String getArtifactName() {
        return artifactName;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    public String getFileName() {
        return FileUtils.getName(getArtifactName());
    }

    public void reset() {
        repositoryId = null;
        currentProjectName = null;
    }
}
