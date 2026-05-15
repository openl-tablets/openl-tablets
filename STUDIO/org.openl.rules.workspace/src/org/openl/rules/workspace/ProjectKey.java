package org.openl.rules.workspace;

import java.util.Objects;

public record ProjectKey(String repositoryId, String repositoryPath) {

    public ProjectKey {
        Objects.requireNonNull(repositoryId, "repositoryId must not be null");
        Objects.requireNonNull(repositoryPath, "repositoryPath must not be null");
    }

}
