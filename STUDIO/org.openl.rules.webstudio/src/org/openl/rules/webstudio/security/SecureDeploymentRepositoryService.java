package org.openl.rules.webstudio.security;

import java.util.List;

import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;

public interface SecureDeploymentRepositoryService {

    List<RepositoryConfiguration> getReadableRepositories();

    List<RepositoryConfiguration> getManageableRepositories();
}
