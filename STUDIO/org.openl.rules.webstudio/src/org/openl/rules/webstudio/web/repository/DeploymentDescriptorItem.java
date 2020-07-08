package org.openl.rules.webstudio.web.repository;

import org.openl.rules.common.CommonVersion;

/**
 * Represents a project in a deployment descriptor.
 */
public class DeploymentDescriptorItem extends AbstractItem {
    private static final long serialVersionUID = -3870494832804679843L;

    private final String repositoryId;
    /** Project version. */
    private CommonVersion version;

    DeploymentDescriptorItem(String repositoryId, String name, CommonVersion version) {
        this(repositoryId, name, version, null);
    }

    private DeploymentDescriptorItem(String repositoryId, String name, CommonVersion version, String messages) {
        this.repositoryId = repositoryId;
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

        return repositoryId.equals(that.repositoryId) && getName().equals(that.getName()) && version.equals(that.version);
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public CommonVersion getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        int result;
        result = getName().hashCode();
        result = 31 * result + repositoryId.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }
}
