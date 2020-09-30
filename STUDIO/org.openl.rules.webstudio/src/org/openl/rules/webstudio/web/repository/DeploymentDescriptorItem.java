package org.openl.rules.webstudio.web.repository;

import java.util.Objects;

import org.openl.rules.common.CommonVersion;

/**
 * Represents a project in a deployment descriptor.
 */
public class DeploymentDescriptorItem extends AbstractItem {
    private static final long serialVersionUID = -3870494832804679843L;

    private final String repositoryId;
    private final String path;
    /** Project version. */
    private final CommonVersion version;

    DeploymentDescriptorItem(String repositoryId, String name, String path, CommonVersion version) {
        this.repositoryId = repositoryId;
        this.path = path;
        setName(name);
        this.version = version;
        setMessages(null);
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

        return Objects.equals(getRepositoryId(), that.getRepositoryId())
                && Objects.equals(getName(), that.getName())
                && Objects.equals(getPath(), that.getPath())
                && Objects.equals(getVersion(), that.getVersion());
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public CommonVersion getVersion() {
        return version;
    }

    public String getPath() {
        return path;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), repositoryId, path, version);
    }
}
