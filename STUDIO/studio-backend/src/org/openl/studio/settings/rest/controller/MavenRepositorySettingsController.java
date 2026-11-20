package org.openl.studio.settings.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.webstudio.dependencies.MavenRepositoryCleanup;
import org.openl.rules.webstudio.web.admin.MavenRepositorySettings;
import org.openl.rules.webstudio.web.admin.SettingsService;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.security.AdminPrivilege;
import org.openl.studio.settings.model.MavenCacheModel;

/**
 * The settings of the Maven repository the dependencies of {@code rules.xml} are downloaded from, and the local
 * repository they are kept in.
 *
 * @author Yury Molchan
 */
@RestController
@RequestMapping("/admin/settings/maven")
@Tag(name = "Settings: Maven Repository")
@Validated
@AdminPrivilege
public class MavenRepositorySettingsController extends PatchSettingsController<MavenRepositorySettings> {

    private final MavenRepositoryCleanup cleanup;

    public MavenRepositorySettingsController(ObjectMapper objectMapper,
                                             BeanValidationProvider validationProvider,
                                             SettingsService settingsService,
                                             MavenRepositoryCleanup cleanup) {
        super(objectMapper, validationProvider, settingsService, MavenRepositorySettings::new);
        this.cleanup = cleanup;
    }

    @GetMapping("/cache")
    @Operation(summary = "msg.settings.maven.cache.get.summary", description = "msg.settings.maven.cache.get.desc")
    public MavenCacheModel getCache() {
        return new MavenCacheModel(cleanup.size());
    }

    @DeleteMapping("/cache")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "msg.settings.maven.cache.delete.summary",
            description = "msg.settings.maven.cache.delete.desc")
    public void deleteCache() {
        cleanup.clear();
    }
}
