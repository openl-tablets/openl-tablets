package org.openl.rules.webstudio.security;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;

public interface SecureDeploymentRepositoryService {

    List<RepositoryConfiguration> getRepositories();

    /**
     * Whether the user can reach a production repository the predicate accepts.
     *
     * <p>Answered one repository at a time, and settled by the first that accepts. Reading a repository's
     * configuration is not free, and a question about any of them is usually answered by the first.
     */
    boolean anyRepository(Predicate<RepositoryConfiguration> accepted);

    Optional<RepositoryConfiguration> getRepository(String id);

    List<RepositoryConfiguration> getManageableRepositories();
}
