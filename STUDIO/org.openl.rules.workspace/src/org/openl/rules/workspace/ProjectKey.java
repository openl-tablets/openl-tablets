package org.openl.rules.workspace;

import java.util.Locale;
import java.util.Objects;

public record ProjectKey(String repositoryId, String repositoryPath) {

    public ProjectKey {
        Objects.requireNonNull(repositoryId, "repositoryId must not be null");
        Objects.requireNonNull(repositoryPath, "repositoryPath must not be null");
    }

    /**
     * The key a project of the repository is looked up by under its name. The case of the name is ignored.
     */
    public static ProjectKey byName(String repositoryId, String name) {
        return new ProjectKey(repositoryId, name.toLowerCase(Locale.ROOT));
    }
}
