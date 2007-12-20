package org.openl.rules.webstudio.web.repository;

import org.openl.rules.repository.CommonVersion;

import java.io.Serializable;


/**
 * Represents a project in a deployment descriptor.
 */
public class DeploymentDescriptorItem implements Serializable {
    private static final long serialVersionUID = -3870494832804679843L;

    /** Project name. */
    private String name;

    /** Project version. */
    private CommonVersion version;

    private String versionName;

    /** Messages associated with this entry. */
    private String messages;

    /** Boolean flag. If this entry is currently 'selected'. */
    private boolean selected;

    public DeploymentDescriptorItem() {}

    public DeploymentDescriptorItem(String name, CommonVersion version) {
        this(name, version, null);
    }

    public DeploymentDescriptorItem(String name, CommonVersion version, String messages) {
        this.name = name;
        this.version = version;
        versionName = version.getVersionName();
        this.messages = messages;
    }

    public CommonVersion getVersion() {
        return version;
    }

    public void setVersion(CommonVersion version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String version) {
        versionName = version;
    }

    public String getMessages() {
        return messages;
    }

    public void setMessages(String messages) {
        this.messages = messages;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeploymentDescriptorItem)) {
            return false;
        }

        DeploymentDescriptorItem that = (DeploymentDescriptorItem) o;

        return name.equals(that.name) && version.equals(that.version);
    }

    public int hashCode() {
        int result;
        result = name.hashCode();
        result = (31 * result) + version.hashCode();
        return result;
    }
}
