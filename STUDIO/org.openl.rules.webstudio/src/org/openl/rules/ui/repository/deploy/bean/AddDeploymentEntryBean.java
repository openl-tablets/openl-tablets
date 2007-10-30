package org.openl.rules.ui.repository.deploy.bean;

import org.openl.rules.ui.repository.UiConst;
import org.openl.rules.ui.repository.deploy.DeploymentDescriptor;

import javax.faces.context.FacesContext;
import javax.faces.application.FacesMessage;

public class AddDeploymentEntryBean {
    private DeploymentDescriptor deploymentDescriptor;

    private String projectName;
    private String version;

    public void setDeploymentDescriptor(DeploymentDescriptorBean deploymentDescriptor) {
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
        if (!deploymentDescriptor.addEntry(new EntryBean(projectName, version))) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage("Deployment descriptor already contains the project"));
        }
        return UiConst.OUTCOME_SUCCESS;
    }
}
