package org.openl.studio.settings.service.repositories;

import java.io.IOException;
import java.util.List;

import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.admin.RepositoryType;
import org.openl.rules.webstudio.web.admin.RepositoryValidationException;
import org.openl.studio.settings.model.repositories.CURepositoryConfigurationModel;

public interface RepositorySettingsService {

    RepositoryConfiguration transform(CURepositoryConfigurationModel request) throws IOException;

    void validate(RepositoryConfiguration configuration) throws RepositoryValidationException;

    List<RepositoryConfiguration> getConfigurations();

    RepositoryConfiguration initialize(RepositoryType type);

    void store(RepositoryConfiguration configuration);

    boolean exists(String id);

    void delete(String id);

    void revert();
}
