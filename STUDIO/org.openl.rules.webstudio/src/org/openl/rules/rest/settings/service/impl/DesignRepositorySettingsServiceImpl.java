package org.openl.rules.rest.settings.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.openl.rules.rest.exception.ForbiddenException;
import org.openl.rules.rest.settings.service.DesignRepositorySettingsService;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.webstudio.web.admin.RepositoryEditor;
import org.openl.rules.webstudio.web.admin.RepositorySettings;

public class DesignRepositorySettingsServiceImpl extends ARepositorySettingsServiceImpl implements DesignRepositorySettingsService {

    private static final Class<?>[] VALIDATION_GROUPS = {RepositorySettings.Validation.Design.class};

    public DesignRepositorySettingsServiceImpl(RepositoryEditor repositoryEditor,
                                               ObjectMapper objectMapper,
                                               BeanValidationProvider beanValidationProvider) {
        super(repositoryEditor, objectMapper, beanValidationProvider);
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectMapper.readerWithView(RepositorySettings.Views.Design.class);
    }

    @Override
    protected Class<?>[] getValidationGroups() {
        return VALIDATION_GROUPS;
    }

    @Override
    public void delete(String id) {
        if (repositoryEditor.getRepositoryConfigurations().size() <= 1) {
            throw new ForbiddenException("At least one repository configuration should be present");
        }
        super.delete(id);
    }
}
