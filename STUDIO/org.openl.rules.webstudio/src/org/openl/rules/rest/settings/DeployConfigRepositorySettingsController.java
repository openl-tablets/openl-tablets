package org.openl.rules.rest.settings;

import java.io.IOException;
import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.rest.settings.model.DesignRepositoryConfigurationModel;
import org.openl.rules.rest.settings.service.DeployConfigRepositorySettingsService;
import org.openl.rules.security.AdminPrivilege;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.admin.RepositorySettings;
import org.openl.rules.webstudio.web.admin.RepositoryValidationException;
import org.openl.rules.webstudio.web.admin.SettingsService;

@RestController
@RequestMapping("/admin/settings/repos/deploy-config")
@Tag(name = "Settings: Deploy Configuration Repository")
@Validated
@Hidden
public class DeployConfigRepositorySettingsController {

    private final DeployConfigRepositorySettingsService repositoryConfigurationService;
    private final SettingsService settingsService;

    public DeployConfigRepositorySettingsController(DeployConfigRepositorySettingsService repositoryConfigurationService,
                                                    SettingsService settingsService) {
        this.repositoryConfigurationService = repositoryConfigurationService;
        this.settingsService = settingsService;
    }

    @GetMapping
    @AdminPrivilege
    @JsonView(RepositorySettings.Views.DeployConfig.class)
    public RepositoryConfiguration getRepositoryConfigurations() {
        return repositoryConfigurationService.getConfiguration();
    }

    @PostMapping(value = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
    @AdminPrivilege
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validateConfiguration(@RequestBody @Valid DesignRepositoryConfigurationModel request) throws RepositoryValidationException, IOException {
        var configuration = repositoryConfigurationService.transform(request);
        repositoryConfigurationService.validate(configuration);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @AdminPrivilege
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateConfiguration(@RequestBody @Valid DesignRepositoryConfigurationModel request) throws IOException, RepositoryValidationException {
        var configuration = repositoryConfigurationService.transform(request);
        repositoryConfigurationService.validate(configuration);
        repositoryConfigurationService.store(configuration);
        settingsService.commit();
    }

    @DeleteMapping
    @AdminPrivilege
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revertConfiguration() throws IOException {
        repositoryConfigurationService.revert();
        settingsService.commit();
    }

}
