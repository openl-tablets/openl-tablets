package org.openl.rules.ui.deploy.controllers;

import org.openl.rules.ui.repository.UiConst;
import org.openl.rules.ui.deploy.DeploymentDescriptor;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

public class AddDeploymentEntryController {
    private DeploymentDescriptor deploymentDescriptor;

    private String projectName;
    private String version;

    public void setDeploymentDescriptor(DeploymentDescriptorController deploymentDescriptor) {
        this.deploymentDescriptor = deploymentDescriptor;
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

    public String add() {
        if (!deploymentDescriptor.addEntry(new EntryController(projectName, version))) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage("Deployment descriptor already contains the project"));
        }
        return UiConst.OUTCOME_SUCCESS;
    }

    public SelectItem[] getAvailableProjects() {
        return new SelectItem[] {
                new SelectItem("Fancy Project"),
                new SelectItem("Clampsy Project"),
                new SelectItem("Mumbo Project"),
                new SelectItem("Jungo Project")
        };
    }

    public SelectItem[] getAvailableVersions() {
        if (projectName == null || projectName.equals("Fancy Project")) {
            return new SelectItem[]{
                    new SelectItem("1.2"),
                    new SelectItem("1.3")
            };
        }

        if (projectName.equals("Clampsy Project")) {
            return new SelectItem[]{
                    new SelectItem("2.1.2"),
                    new SelectItem("2.3.1"),
                    new SelectItem("2.3.3")
            };
        }

        return new SelectItem[] {
                new SelectItem("3.3.1"),
                new SelectItem("3.3.2"),
                new SelectItem("3.3.3")
        };
    }
}
