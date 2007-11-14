package org.openl.rules.webstudio.web.deployment;

import static org.openl.rules.ui.repository.UiConst.OUTCOME_SUCCESS;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;


/**
 * Deployment controller.
 *
 * @author Andrey Naumenko
 */
public class DeploymentController {
    private List<DeploymentDescriptorItem> items;
    private String projectName;
    private String version;

    public List<DeploymentDescriptorItem> getItems() {
        items = new ArrayList<DeploymentDescriptorItem>();
        items.add(new DeploymentDescriptorItem("Project 1", "1.2.1"));
        items.add(new DeploymentDescriptorItem("Project 2", "1.2.2",
                "Conflicts with project 5 v1.0.4"));
        items.add(new DeploymentDescriptorItem("Project 5", "1.0.4"));
        return items;
    }

    public void setItems(List<DeploymentDescriptorItem> items) {
        this.items = items;
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
        items.add(new DeploymentDescriptorItem(projectName, version));
        return OUTCOME_SUCCESS;
    }

    public SelectItem[] getAvailableProjects() {
        return new SelectItem[] {
            new SelectItem("Fancy Project"), new SelectItem("Clampsy Project"),
            new SelectItem("Mumbo Project"), new SelectItem("Jungo Project")
        };
    }

    public SelectItem[] getAvailableVersions() {
        if ((projectName == null) || projectName.equals("Fancy Project")) {
            return new SelectItem[] { new SelectItem("1.2"), new SelectItem("1.3") };
        }

        if (projectName.equals("Clampsy Project")) {
            return new SelectItem[] {
                new SelectItem("2.1.2"), new SelectItem("2.3.1"), new SelectItem("2.3.3")
            };
        }

        return new SelectItem[] {
            new SelectItem("3.3.1"), new SelectItem("3.3.2"), new SelectItem("3.3.3")
        };
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
