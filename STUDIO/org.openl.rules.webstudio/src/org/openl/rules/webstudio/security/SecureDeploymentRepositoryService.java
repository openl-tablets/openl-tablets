package org.openl.rules.webstudio.security;

import java.util.List;
import java.util.Optional;

import org.springframework.security.acls.model.Permission;

import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;

public interface SecureDeploymentRepositoryService {

    List<RepositoryConfiguration> getRepositories();

    Optional<RepositoryConfiguration> getRepository(String id);

    List<RepositoryConfiguration> getManageableRepositories();

    boolean hasPermission(Permission permission);
}
