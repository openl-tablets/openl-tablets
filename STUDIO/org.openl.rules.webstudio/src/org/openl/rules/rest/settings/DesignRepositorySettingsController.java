package org.openl.rules.rest.settings;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.rest.settings.service.DesignRepositorySettingsService;
import org.openl.rules.webstudio.web.admin.RepositorySettings;
import org.openl.rules.webstudio.web.admin.SettingsService;

@RestController
@RequestMapping("/admin/settings/repos/design")
@Tag(name = "Settings: Design Repository")
@Validated
public class DesignRepositorySettingsController extends CRUDRepositorySettingsController {

    public DesignRepositorySettingsController(SettingsService settingsService,
                                              DesignRepositorySettingsService designRepositoryConfigService) {
        super(settingsService, designRepositoryConfigService);
    }

    @Override
    protected Class<?> getViewClass() {
        return RepositorySettings.Views.Design.class;
    }
}
