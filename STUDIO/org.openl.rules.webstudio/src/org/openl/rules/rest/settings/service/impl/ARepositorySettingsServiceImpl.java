package org.openl.rules.rest.settings.service.impl;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.openl.rules.rest.settings.model.CURepositoryConfigurationModel;
import org.openl.rules.rest.settings.service.RepositorySettingsService;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.admin.RepositoryEditor;
import org.openl.rules.webstudio.web.admin.RepositoryType;
import org.openl.rules.webstudio.web.admin.RepositoryValidationException;
import org.openl.studio.common.exception.NotFoundException;

public abstract class ARepositorySettingsServiceImpl implements RepositorySettingsService {

    protected final RepositoryEditor repositoryEditor;
    protected final ObjectMapper objectMapper;
    protected final BeanValidationProvider beanValidationProvider;

    protected ARepositorySettingsServiceImpl(RepositoryEditor repositoryEditor,
                                             ObjectMapper objectMapper,
                                             BeanValidationProvider beanValidationProvider) {
        this.repositoryEditor = repositoryEditor;
        this.objectMapper = objectMapper;
        this.beanValidationProvider = beanValidationProvider;
    }

    @Override
    public RepositoryConfiguration transform(CURepositoryConfigurationModel request) throws IOException {
        var existingConfig = repositoryEditor.getRepositoryConfiguration(request.getId());
        RepositoryConfiguration configuration;
        if (existingConfig.isEmpty()) {
            configuration = repositoryEditor.initializeConfiguration(request.getType());
        } else {
            configuration = existingConfig.get();
            configuration.setType(request.getType().factoryId);
        }
        configuration.setName(request.getName());
        getObjectReader().withValueToUpdate(configuration.getSettings())
                .readValue(request.getSettings());
        return configuration;
    }

    @Override
    public void validate(RepositoryConfiguration configuration) throws RepositoryValidationException {
        beanValidationProvider.validate(configuration, getValidationGroups());
        repositoryEditor.validate(configuration);
    }

    protected Class<?>[] getValidationGroups() {
        return null;
    }

    @Override
    public List<RepositoryConfiguration> getConfigurations() {
        return repositoryEditor.getRepositoryConfigurations();
    }

    @Override
    public RepositoryConfiguration initialize(RepositoryType type) {
        return repositoryEditor.initializeConfiguration(type);
    }

    @Override
    public void store(RepositoryConfiguration configuration) {
        var existingConfig = repositoryEditor.getRepositoryConfiguration(configuration.getId());
        if (existingConfig.isEmpty()) {
            repositoryEditor.addRepository(configuration);
        }
        repositoryEditor.save();
    }

    @Override
    public boolean exists(String id) {
        return repositoryEditor.getRepositoryConfiguration(id).isPresent();
    }

    @Override
    public void delete(String id) {
        var existingConfig = repositoryEditor.getRepositoryConfiguration(id);
        if (existingConfig.isEmpty()) {
            throw new NotFoundException("repository.message", id);
        }
        repositoryEditor.deleteRepository(existingConfig.get().getId());
        repositoryEditor.save();
    }

    @Override
    public void revert() {
        repositoryEditor.revertChanges();
    }

    protected abstract ObjectReader getObjectReader();
}
