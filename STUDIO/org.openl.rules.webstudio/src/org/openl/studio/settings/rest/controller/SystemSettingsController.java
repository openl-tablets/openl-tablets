package org.openl.studio.settings.rest.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.webstudio.web.admin.AdministrationSettings;
import org.openl.rules.webstudio.web.admin.SettingsService;
import org.openl.studio.common.validation.BeanValidationProvider;

@RestController
@RequestMapping("/admin/settings/system")
@Tag(name = "Settings: System")
@Validated
public class SystemSettingsController extends CRUDSettingsController<AdministrationSettings> {

    public SystemSettingsController(ObjectMapper objectMapper, BeanValidationProvider validationProvider,
                                    SettingsService settingsService) {
        super(objectMapper, validationProvider, settingsService, AdministrationSettings::new);
    }
}
