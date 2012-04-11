package org.openl.rules.webstudio.web.repository;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.webstudio.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectDependency;
import org.openl.rules.workspace.abstracts.ProjectDescriptor;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.ProductionDeployer;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceDeploymentProject;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.rules.workspace.uw.impl.UserWorkspaceDeploymentProjectImpl;
import org.openl.rules.workspace.uw.impl.UserWorkspaceProjectDescriptorImpl;
import org.openl.rules.workspace.uw.impl.UserWorkspaceImpl;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryProject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;


/**
 * Deployment controller.
 *
 * @author Andrey Naumenko
 */
public class DeploymentController {
    private final static Log log = LogFactory.getLog(DeploymentController.class);
    private List<DeploymentDescriptorItem> items;
    private String projectName;
    private String version;
    private RepositoryTreeState repositoryTreeState;
    private String cachedForProject;

    public synchronized List<DeploymentDescriptorItem> getItems() {
        UserWorkspaceDeploymentProject project = getSelectedProject();
        if (project == null) {
            return null;
        }

        if (items != null && project.getName().equals(cachedForProject))
            return items;

        cachedForProject = project.getName();
        Collection<ProjectDescriptor> descriptors = project.getProjectDescriptors();
        items = new ArrayList<DeploymentDescriptorItem>();

        for (ProjectDescriptor descriptor : descriptors) {
            DeploymentDescriptorItem item = new DeploymentDescriptorItem(descriptor
                    .getProjectName(), descriptor.getProjectVersion());
            items.add(item);
        }

        try {
            checkConflicts(items);
        } catch (ProjectException e) {
            log.error(e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(),
                        e.getMessage()));
        }

        return items;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public synchronized String addItem() {
        UserWorkspaceDeploymentProject project = getSelectedProject();

        UserWorkspaceProjectDescriptorImpl newItem = new UserWorkspaceProjectDescriptorImpl((UserWorkspaceDeploymentProjectImpl) project,
                projectName, new CommonVersionImpl(version));
        List<ProjectDescriptor> newDescriptors = replaceDescriptor(project, projectName,
                newItem);

        try {
            project.setProjectDescriptors(newDescriptors);
        } catch (ProjectException e) {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "failed to add project descriptor", e.getMessage()));
            log.error(e);
        }

        return null;
    }

    private List<ProjectDescriptor> replaceDescriptor(
        UserWorkspaceDeploymentProject project, String projectName,
        UserWorkspaceProjectDescriptorImpl newItem) {
        List<ProjectDescriptor> newDescriptors = new ArrayList<ProjectDescriptor>();

        for (ProjectDescriptor pd : project.getProjectDescriptors()) {
            if (pd.getProjectName().equals(projectName)) {
                if (newItem != null) {
                    newDescriptors.add(newItem);
                    newItem = null;
                }
            } else {
                newDescriptors.add(pd);
            }
        }
        if (newItem != null) {
            newDescriptors.add(newItem);
        }
        items = null;
        return newDescriptors;
    }

    public String deleteItem() {
        String projectName = FacesUtils.getRequestParameter("key");
        UserWorkspaceDeploymentProject project = getSelectedProject();

        try {
            project.setProjectDescriptors(replaceDescriptor(project, projectName, null));
        } catch (ProjectException e) {
            log.error(e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "failed to add project descriptor", e.getMessage()));
        }
        return null;
    }

    private UserWorkspaceDeploymentProject getSelectedProject() {
        ProjectArtefact artefact = repositoryTreeState.getSelectedNode().getDataBean();
        if (artefact instanceof UserWorkspaceDeploymentProject) {
            return (UserWorkspaceDeploymentProject) artefact;
        }
        return null;
    }

    private UserWorkspace getWorkspace() {
        try {
            return getRulesUserSession().getUserWorkspace();
        } catch (Exception e) {
            log.error("Error obtaining user workspace", e);
        }
        return null;
    }

    private RulesUserSession getRulesUserSession() {
        return (RulesUserSession) FacesUtils.getSessionMap().get("rulesUserSession");
    }

    public SelectItem[] getProjects() {
        UserWorkspace workspace = getWorkspace();
        Collection<UserWorkspaceProject> workspaceProjects = workspace.getProjects();
        List<SelectItem> selectItems = new ArrayList<SelectItem>();

        List<DeploymentDescriptorItem> existingItems = getItems();
        Set<String> existing = new HashSet<String>();
        if (existingItems != null) {
            for (DeploymentDescriptorItem ddItem : existingItems) {
                existing.add(ddItem.getName());
            }
        }

        for (UserWorkspaceProject project : workspaceProjects) {
            if (!(project.isDeploymentProject() || existing.contains(project.getName())
                    || project.isLocalOnly())) {
                selectItems.add(new SelectItem(project.getName()));
            }
        }

        return selectItems.toArray(new SelectItem[selectItems.size()]);
    }

    public SelectItem[] getProjectVersions() {
        UserWorkspace workspace = getWorkspace();
        if (projectName != null) {
            try {
                UserWorkspaceProject project = workspace.getProject(projectName);
                // sort project versions in descending order (1.1 -> 0.0)
                List<ProjectVersion> versions = new ArrayList<ProjectVersion>(project.getVersions());
                Collections.sort(versions, RepositoryUtils.VERSIONS_REVERSE_COMPARATOR);

                List<SelectItem> selectItems = new ArrayList<SelectItem>();
                for (ProjectVersion version : versions) {
                    selectItems.add(new SelectItem(version.getVersionName()));
                }
                return selectItems.toArray(new SelectItem[selectItems.size()]);
            } catch (ProjectException e) {
                log.error(e);
            }
        }
        return new SelectItem[0];
    }

    public String openSelectedProjects() {
        UserWorkspace workspace = getWorkspace();
        for (DeploymentDescriptorItem item : items) {
            if (item.isSelected()) {
                String projectName = item.getName();
                try {
                    UserWorkspaceProject project = workspace.getProject(projectName);
                    if (!project.isOpened()) {
                        project.open();
                    }
                } catch (ProjectException e) {
                    log.error("Failed to open project " + projectName + " "
                        + e.getMessage());
                }
            }
            item.setSelected(false);
        }
        return null;
    }

    public String deploy() {
        UserWorkspaceDeploymentProject project = getSelectedProject();
        if (project != null) {
            try {
                ProductionDeployer deployer = getRulesUserSession().getDeployer();
                DesignTimeRepository dtr = getWorkspace().getDesignTimeRepository();

                Collection<ProjectDescriptor> projectDescriptors = project
                        .getProjectDescriptors();
                Collection<Project> projects = new ArrayList<Project>();

                for (ProjectDescriptor pd : projectDescriptors) {
                    projects.add(dtr.getProject(pd.getProjectName(), pd.getProjectVersion()));
                }

                DeployID id = getDeployID(project);
                deployer.deploy(id, projects);

                FacesContext.getCurrentInstance()
                    .addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "project successfully deployed with id: " + id.getName(), null));
            } catch (Exception e) {
                FacesContext.getCurrentInstance()
                    .addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "failed to deploy",
                            e.getMessage()));
                log.error(e);
            }
        }
        return null;
    }

    private void checkConflicts(List<DeploymentDescriptorItem> items)
        throws ProjectException
    {
        if (items == null) {
            return;
        }
        final UserWorkspace workspace = getWorkspace();
        final DesignTimeRepository dtr = workspace.getDesignTimeRepository();

        int n = items.size();
        Map<String, VersionRange>[] projects = new Map[n];
        for (int i = 0; i < items.size(); i++) {
            DeploymentDescriptorItem ddItem = items.get(i);
            ddItem.setMessages("");

            Map<String, VersionRange> prMap = projects[i] = new HashMap<String, VersionRange>();

            String projectName = ddItem.getName();

            if (!workspace.hasProject(projectName)) {
                // no such project (was deleted?)
                prMap.put(projectName, null);

                ddItem.setMessages(new StringBuilder().append(
                    "<span style='color:red;'>Cannot find project <b>")
                    .append(StringEscapeUtils.escapeHtml(projectName))
                    .append("</b> in the repository</span>.").toString());
            } else {
                RepositoryProject repositoryProject = dtr.getProject(projectName, ddItem.getVersion());

                prMap.put(repositoryProject.getName(), new VersionRange(repositoryProject.getVersion(),
                        repositoryProject.getVersion()));

                // fill project's dependencies
                for (ProjectDependency dep : repositoryProject.getDependencies()) {
                    prMap.put(dep.getProjectName(), new VersionRange(dep.getLowerLimit(), dep.getUpperLimit()));
                }
            }
        }

outer:
        for (int i = 0; i < n; ++i) {
            DeploymentDescriptorItem deploymentDescriptorItem = items.get(i);
            for (Map.Entry<String, VersionRange> entries : projects[i].entrySet()) {
                VersionRange range = entries.getValue();
                for (int j = 0; j < n; ++j) {
                    if (i != j) {
                        VersionRange otherRange = projects[j].get(entries.getKey());
                        if ((otherRange != null) && !range.intersects(otherRange)) {
                            deploymentDescriptorItem.setMessages(new StringBuilder().append(
                                    "<span style='color:red;'>Conflicting with project <b>")
                                    .append(StringEscapeUtils.escapeHtml(items.get(j)
                                            .getName()))
                                    .append("</b></span>. Dependency causing conflict: <b>")
                                    .append(StringEscapeUtils.escapeHtml(entries.getKey()))
                                    .append("</b>").toString());
                            continue outer; // one message is enough
                        }
                    }
                }
            }
        }
    }

    private static DeployID getDeployID(UserWorkspaceDeploymentProject ddProject) {
        StringBuilder sb = new StringBuilder(ddProject.getName());
        ProjectVersion projectVersion = ddProject.getVersion();
        if (projectVersion != null) {
            sb.append('#').append(projectVersion.getVersionName());
        }
        return new DeployID(sb.toString());
    }

    public String checkIn() {
        try {
            getSelectedProject().checkIn();
            items = null;
        } catch (ProjectException e) {
            log.error(e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "failed to check in",
                        e.getMessage()));
        }

        return null;
    }

    public String close() {
        try {
            getSelectedProject().close();
            items = null;
        } catch (ProjectException e) {
            log.error(e);
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "failed to close deployment project", e.getMessage()));
        }

        return null;
    }

    public String checkOut() {
        try {
            getSelectedProject().checkOut();
            items = null;
        } catch (ProjectException e) {
            FacesContext.getCurrentInstance()
                .addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "failed to check out",
                        e.getMessage()));
            log.error(e);
        }

        return null;
    }

    public boolean isCheckinable() {
        return true;
    }

    public boolean isCheckoutable() {
        return true;
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    private static class VersionRange {
        CommonVersion lower;
        CommonVersion upper;

        private VersionRange(CommonVersion lower, CommonVersion upper) {
            this.lower = lower;
            this.upper = upper;
        }

        private int compare(int i, int j) {
            return (i == j) ? 0 : ((i < j) ? (-1) : 1);
        }

        public int compare(CommonVersion o1, CommonVersion o2) {
            if ((o1 == null) || (o2 == null)) {
                // null stands for posistive infinity
                if (o1 != null) {
                    return -1;
                }
                if (o2 != null) {
                    return 1;
                }
                return 0;
            }

            int c = compare(o1.getMajor(), o2.getMajor());
            if (c != 0) {
                return c;
            }
            c = compare(o1.getMinor(), o2.getMinor());
            if (c != 0) {
                return c;
            }
            return compare(o1.getRevision(), o2.getRevision());
        }

        boolean intersects(VersionRange other) {
            return (compare(lower, other.upper) <= 0)
                && (compare(other.lower, upper) <= 0);
        }
    }
}
