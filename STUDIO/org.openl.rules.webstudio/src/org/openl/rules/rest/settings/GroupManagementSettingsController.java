package org.openl.rules.rest.settings;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.webstudio.web.admin.GroupManagementSettings;
import org.openl.rules.webstudio.web.admin.SettingsService;

@RestController
@RequestMapping("/admin/settings/group-management")
@Tag(name = "Settings: Group Management")
@Validated
public class GroupManagementSettingsController extends CRUDSettingsController<GroupManagementSettings> {

    public GroupManagementSettingsController(ObjectMapper objectMapper, BeanValidationProvider validationProvider,
                                             SettingsService settingsService) {
        super(objectMapper, validationProvider, settingsService, GroupManagementSettings::new);
    }
}
