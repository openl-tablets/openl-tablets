package org.openl.rules.rest.settings.service;

import java.io.IOException;

import org.openl.rules.rest.settings.model.DeployConfigRepositoryConfigurationModel;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.admin.RepositoryValidationException;

public interface DeployConfigRepositorySettingsService {

    RepositoryConfiguration getConfiguration();

    RepositoryConfiguration transform(DeployConfigRepositoryConfigurationModel request) throws IOException;

    void validate(RepositoryConfiguration configuration) throws RepositoryValidationException;

    void store(RepositoryConfiguration configuration);

    void revert();

}
