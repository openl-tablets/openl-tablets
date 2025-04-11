package org.openl.rules.rest.settings;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.rest.validation.BeanValidationProvider;
import org.openl.rules.webstudio.web.admin.MailVerificationServerSettings;
import org.openl.rules.webstudio.web.admin.SettingsService;

@RestController
@RequestMapping("/admin/settings/mail")
@Tag(name = "Settings: Mail Verification Server")
@Validated
public class MailVerificationServerSettingsController extends CRUDSettingsController<MailVerificationServerSettings> {

    public MailVerificationServerSettingsController(ObjectMapper objectMapper, BeanValidationProvider validationProvider,
                                                    SettingsService settingsService) {
        super(objectMapper, validationProvider, settingsService, MailVerificationServerSettings::new);
    }
}
