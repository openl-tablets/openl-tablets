package org.openl.studio.settings.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.webstudio.web.admin.MavenRepositorySettings;
import org.openl.rules.webstudio.web.admin.SettingsService;
import org.openl.studio.common.validation.BeanValidationProvider;

/**
 * The settings of the Maven repository the dependencies of {@code rules.xml} are downloaded from.
 *
 * @author Yury Molchan
 */
@RestController
@RequestMapping("/admin/settings/maven")
@Tag(name = "Settings: Maven Repository")
@Validated
public class MavenRepositorySettingsController extends CRUDSettingsController<MavenRepositorySettings> {

    public MavenRepositorySettingsController(ObjectMapper objectMapper,
                                             BeanValidationProvider validationProvider,
                                             SettingsService settingsService) {
        super(objectMapper, validationProvider, settingsService, MavenRepositorySettings::new);
    }
}
