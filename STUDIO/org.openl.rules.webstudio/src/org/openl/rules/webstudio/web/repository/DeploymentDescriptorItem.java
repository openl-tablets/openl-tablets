package org.openl.rules.webstudio.web.repository;

import org.openl.rules.common.CommonVersion;

/**
 * Represents a project in a deployment descriptor.
 */
public class DeploymentDescriptorItem extends AbstractItem {
    private static final long serialVersionUID = -3870494832804679843L;

    /** Project version. */
    private CommonVersion version;

    DeploymentDescriptorItem(String name, CommonVersion version) {
        this(name, version, null);
    }

    private DeploymentDescriptorItem(String name, CommonVersion version, String messages) {
        setName(name);
        this.version = version;
        setMessages(messages);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeploymentDescriptorItem)) {
            return false;
        }

        DeploymentDescriptorItem that = (DeploymentDescriptorItem) o;

        return getName().equals(that.getName()) && version.equals(that.version);
    }

    public CommonVersion getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        int result;
        result = getName().hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }
}
