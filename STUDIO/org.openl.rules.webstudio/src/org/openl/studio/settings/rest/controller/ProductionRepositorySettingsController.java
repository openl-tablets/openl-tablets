package org.openl.studio.settings.rest.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.webstudio.web.admin.RepositorySettings;
import org.openl.rules.webstudio.web.admin.SettingsService;
import org.openl.studio.settings.service.repositories.ProductionRepositorySettingsService;

@RestController
@RequestMapping("/admin/settings/repos/production")
@Tag(name = "Settings: Production Repository")
@Validated
public class ProductionRepositorySettingsController extends CRUDRepositorySettingsController {

    public ProductionRepositorySettingsController(SettingsService settingsService,
                                                  ProductionRepositorySettingsService repositoryConfigurationService) {
        super(settingsService, repositoryConfigurationService);
    }

    @Override
    protected Class<?> getViewClass() {
        return RepositorySettings.Views.Production.class;
    }

}
