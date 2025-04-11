package org.openl.rules.rest.settings.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.openl.rules.rest.settings.service.ProductionRepositorySettingsService;
import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.webstudio.web.admin.RepositoryEditor;
import org.openl.rules.webstudio.web.admin.RepositorySettings;

public class ProductionRepositorySettingsServiceImpl extends ARepositorySettingsServiceImpl implements ProductionRepositorySettingsService {

    public ProductionRepositorySettingsServiceImpl(RepositoryEditor repositoryEditor,
                                                   ObjectMapper objectMapper,
                                                   BeanValidationProvider beanValidationProvider) {
        super(repositoryEditor, objectMapper, beanValidationProvider);
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectMapper.readerWithView(RepositorySettings.Views.Production.class);
    }

}
