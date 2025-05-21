package org.openl.rules.rest.settings.service.impl;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.openl.config.PropertiesHolder;
import org.openl.rules.repository.RepositoryMode;
import org.openl.rules.rest.settings.model.DeployConfigRepositoryConfigurationModel;
import org.openl.rules.rest.settings.service.DeployConfigRepositorySettingsService;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.admin.RepositoryEditor;
import org.openl.rules.webstudio.web.admin.RepositorySettings;
import org.openl.rules.webstudio.web.admin.RepositoryValidationException;
import org.openl.rules.webstudio.web.admin.RepositoryValidators;
import org.openl.util.StringUtils;

public class DeployConfigRepositorySettingsServiceImpl implements DeployConfigRepositorySettingsService {

    private final PropertiesHolder propertiesHolder;
    private final ObjectMapper objectMapper;
    private final BeanValidationProvider beanValidationProvider;
    private final RepositoryEditor designRepositoryEditor;

    public DeployConfigRepositorySettingsServiceImpl(PropertiesHolder propertiesHolder,
                                                     ObjectMapper objectMapper,
                                                     BeanValidationProvider beanValidationProvider,
                                                     RepositoryEditor designRepositoryEditor) {
        this.propertiesHolder = propertiesHolder;
        this.objectMapper = objectMapper;
        this.beanValidationProvider = beanValidationProvider;
        this.designRepositoryEditor = designRepositoryEditor;
    }

    @Override
    public RepositoryConfiguration getConfiguration() {
        return new RepositoryConfiguration(RepositoryMode.DEPLOY_CONFIG.getId(), propertiesHolder);
    }

    @Override
    public RepositoryConfiguration transform(DeployConfigRepositoryConfigurationModel request) throws IOException {
        var configuration = getConfiguration();
        if (request.getType() != null) {
            configuration.setType(request.getType().factoryId);
        }
        configuration.setName(request.getName());
        configuration.setUseDesignRepositoryForDeployConfig(request.getUseDesignRepositoryForDeployConfig());
        objectMapper.readerWithView(RepositorySettings.Views.DeployConfig.class)
                .withValueToUpdate(configuration.getSettings())
                .readValue(request.getSettings());
        return configuration;
    }

    @Override
    public void validate(RepositoryConfiguration configuration) throws RepositoryValidationException {
        beanValidationProvider.validate(configuration);
        var designRepositoryForDeployConfig = configuration.getUseDesignRepositoryForDeployConfig();
        boolean useSeparateDeployConfigRepo = StringUtils.isNotBlank(designRepositoryForDeployConfig);
        if (useSeparateDeployConfigRepo) {
            var designRepository = designRepositoryEditor.getRepositoryConfiguration(designRepositoryForDeployConfig);
            if (designRepository.isEmpty()) {
                throw new RepositoryValidationException("Design repository not found: " + designRepositoryForDeployConfig);
            }
        } else {
            RepositoryValidators.validate(configuration);
            RepositoryValidators.validateConnection(configuration);
        }
    }

    @Override
    public void store(RepositoryConfiguration configuration) {
        configuration.commit();
    }

    @Override
    public void revert() {
        getConfiguration().revert();
    }
}
