package org.openl.rules.workspace;

import java.util.Objects;

public final class ProjectKey {
    private final String repositoryId;
    private final String repositoryPath;

    public ProjectKey(String repositoryId, String repositoryPath) {
        Objects.requireNonNull(repositoryId);
        Objects.requireNonNull(repositoryPath);
        this.repositoryId = repositoryId;
        this.repositoryPath = repositoryPath;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public String getRepositoryPath() {
        return repositoryPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ProjectKey that = (ProjectKey) o;
        return repositoryId.equals(that.repositoryId) && repositoryPath.equals(that.repositoryPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repositoryId, repositoryPath);
    }
}
