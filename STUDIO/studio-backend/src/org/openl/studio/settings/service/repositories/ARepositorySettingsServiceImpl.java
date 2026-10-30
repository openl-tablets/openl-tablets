package org.openl.studio.settings.service.repositories;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.admin.RepositoryEditor;
import org.openl.rules.webstudio.web.admin.RepositoryType;
import org.openl.rules.webstudio.web.admin.RepositoryValidationException;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.settings.converter.ReadOnlySettings;
import org.openl.studio.settings.model.repositories.CURepositoryConfigurationModel;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ARepositorySettingsServiceImpl implements RepositorySettingsService {

    protected final RepositoryEditor repositoryEditor;
    protected final ObjectMapper objectMapper;
    protected final BeanValidationProvider beanValidationProvider;

    @Override
    public RepositoryConfiguration transform(CURepositoryConfigurationModel request) throws IOException {
        var existingConfig = repositoryEditor.getRepositoryConfiguration(request.getId());
        RepositoryConfiguration configuration;
        if (existingConfig.isEmpty()) {
            checkListEditable();
            configuration = repositoryEditor.initializeConfiguration(request.getType());
        } else {
            configuration = existingConfig.get();
            ReadOnlySettings.check(ReadOnlySettings.answerOf(objectMapper, configuration), requested(request));
            configuration.setType(request.getType().factoryId);
        }
        configuration.setName(request.getName());
        getObjectReader().withValueToUpdate(configuration.getSettings())
                .readValue(request.getSettings());
        return configuration;
    }

    /**
     * The request in the shape the repository is answered in.
     */
    private JsonNode requested(CURepositoryConfigurationModel request) {
        var requested = objectMapper.createObjectNode();
        requested.put("name", request.getName());
        requested.put("type", request.getType().factoryId);
        requested.set("settings", request.getSettings());
        return requested;
    }

    /**
     * Refuses to add or remove a repository while an environment variable or a system property sets the list of the
     * repositories.
     */
    private void checkListEditable() {
        if (isListReadOnly()) {
            throw new BadRequestException("settings.repositories.read-only.message",
                    new Object[]{repositoryEditor.getListProperty()});
        }
    }

    @Override
    public void validate(RepositoryConfiguration configuration) throws RepositoryValidationException {
        beanValidationProvider.validate(configuration, getValidationGroups());
        repositoryEditor.validate(configuration);
    }

    protected Class<?>[] getValidationGroups() {
        return BeanValidationProvider.EMPTY_GROUPS;
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
        checkListEditable();
        repositoryEditor.deleteRepository(existingConfig.get().getId());
        repositoryEditor.save();
    }

    @Override
    public void revert() {
        repositoryEditor.revertChanges();
    }

    @Override
    public boolean isListReadOnly() {
        return repositoryEditor.isListReadOnly();
    }

    protected abstract ObjectReader getObjectReader();
}
