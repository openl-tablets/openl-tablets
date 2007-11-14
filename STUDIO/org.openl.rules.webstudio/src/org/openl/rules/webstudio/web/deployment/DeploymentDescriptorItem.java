package org.openl.rules.webstudio.web.deployment;

import java.io.Serializable;

/**
 * Represents a project in a deployment descriptor.
 */
public class DeploymentDescriptorItem implements Serializable {
    /**
     * Project name.
     */
    private String name;
    /**
     * Project version.
     */
    private String version;
    /**
     * Messages associated with this entry.
     */
    private String messages;
    /**
     * Boolean flag. If this entry is currently 'selected'.
     */
    private boolean selected;

    public DeploymentDescriptorItem() {}

    public DeploymentDescriptorItem(String name, String version) {
        this(name, version, null);
    }

    public DeploymentDescriptorItem(String name, String version, String messages) {
        this.name = name;
        this.version = version;
        this.messages = messages;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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
        if (this == o) return true;
        if (!(o instanceof DeploymentDescriptorItem)) return false;

        DeploymentDescriptorItem that = (DeploymentDescriptorItem) o;

        return name.equals(that.name) && version.equals(that.version);

    }

    public int hashCode() {
        int result;
        result = name.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }
}
