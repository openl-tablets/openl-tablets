package org.openl.rules.ui.repository;

import static org.openl.rules.ui.repository.UiConst.OUTCOME_SUCCESS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.webstudio.RulesUserSession;
import org.openl.rules.webstudio.util.FacesUtils;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceDeploymentProject;
import org.openl.rules.workspace.uw.UserWorkspaceProject;


/**
 * Deployment controller.
 *
 * @author Andrey Naumenko
 */
public class DeploymentController implements Serializable {
    private final static Log log = LogFactory.getLog(DeploymentController.class);
    private List<DeploymentDescriptorItem> items;
    private String projectName;
    private String version;

    public DeploymentController() {
        items = new ArrayList<DeploymentDescriptorItem>();
        items.add(new DeploymentDescriptorItem("Project 1", "1.2.1"));
        items.add(new DeploymentDescriptorItem("Project 2", "1.2.2",
                "Conflicts with project 5 v1.0.4"));
        items.add(new DeploymentDescriptorItem("Project 5", "1.0.4"));
    }

    public List<DeploymentDescriptorItem> getItems() {
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

    public String addItem() {
        DeploymentDescriptorItem newItem = new DeploymentDescriptorItem(projectName,
                version);
        if (!items.contains(newItem)) {
            items.add(newItem);
        }
        return OUTCOME_SUCCESS;
    }

    public String deleteItem() {
        Integer key = Integer.valueOf(FacesUtils.getRequestParameter("key"));
        items.remove(key.intValue());
        return OUTCOME_SUCCESS;
    }

    public String save() {
        UserWorkspace workspace = getWorkspace();

        String name = "123";

        try {
            workspace.createDDProject(name);

            UserWorkspaceDeploymentProject ddp = workspace.getDDProject(name);
            ddp.checkOut();

            for (DeploymentDescriptorItem item : items) {
                String[] version = StringUtils.split(item.getVersion(), '.');
                int major = 0;
                int minor = 0;
                int revision = 0;
                if (version.length > 0) {
                    major = Integer.parseInt(version[0]);
                }
                if (version.length > 1) {
                    minor = Integer.parseInt(version[1]);
                }
                if (version.length > 2) {
                    revision = Integer.parseInt(version[2]);
                }
                
                ddp.addProjectDescriptor(item.getName(), new CommonVersionImpl(major, minor, revision));
            }

            ddp.checkIn();
        } catch (Exception e) {
            log.error("Cannot create new DDP " + name);
            return null;
        }

        return null;
    }

    private UserWorkspace getWorkspace() {
        RulesUserSession rulesUserSession = (RulesUserSession) FacesUtils.getSessionMap()
                .get("rulesUserSession");

        UserWorkspace workspace = null;

        try {
            workspace = rulesUserSession.getUserWorkspace();
        } catch (Exception e) {
            log.error("Error obtaining user workspace", e);
            return null;
        }
        return workspace;
    }

    public SelectItem[] getProjects() {
        UserWorkspace workspace = getWorkspace();
        Collection<UserWorkspaceProject> projects = workspace.getProjects();
        List<SelectItem> selectItems = new ArrayList<SelectItem>();
        for (UserWorkspaceProject project : projects) {
            selectItems.add(new SelectItem(project.getName()));
            if (projectName == null) {
                projectName = project.getName();
            }
        }

        return selectItems.toArray(new SelectItem[0]);
    }

    public SelectItem[] getProjectVersions() {
        UserWorkspace workspace = getWorkspace();
        if (projectName == null) {
            return new SelectItem[0];
        }

        try {
            UserWorkspaceProject project = workspace.getProject(projectName);

            List<SelectItem> selectItems = new ArrayList<SelectItem>();
            for (ProjectVersion version : project.getVersions()) {
                selectItems.add(new SelectItem(version.getVersionName()));
            }
            return selectItems.toArray(new SelectItem[0]);
        } catch (ProjectException e) {
            log.error(e);
        }

        return null;
    }

    public String deploy() {
        
        return OUTCOME_SUCCESS;
    }

    public String checkIn() {
        return OUTCOME_SUCCESS;
    }

    public String checkOut() {
        return OUTCOME_SUCCESS;
    }

    public boolean isCheckinable() {
        return true;
    }

    public boolean isCheckoutable() {
        return true;
    }
}
