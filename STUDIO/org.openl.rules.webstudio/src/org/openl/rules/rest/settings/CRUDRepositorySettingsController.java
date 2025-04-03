package org.openl.rules.rest.settings;

import java.io.IOException;
import javax.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import org.openl.rules.rest.settings.model.CURepositoryConfigurationModel;
import org.openl.rules.rest.settings.model.CreateRepositoryTemplateModel;
import org.openl.rules.rest.settings.service.RepositorySettingsService;
import org.openl.rules.security.AdminPrivilege;
import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.admin.RepositoryValidationException;
import org.openl.rules.webstudio.web.admin.SettingsService;

public abstract class CRUDRepositorySettingsController {

    protected final SettingsService settingsService;
    protected final RepositorySettingsService repositoryConfigurationService;

    public CRUDRepositorySettingsController(SettingsService settingsService,
                                            RepositorySettingsService repositoryConfigurationService) {
        this.settingsService = settingsService;
        this.repositoryConfigurationService = repositoryConfigurationService;
    }

    @Operation(description = "msg.repository-settings.get-all.desc", summary = "msg.repository-settings.get-all.summary")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @AdminPrivilege
    @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = RepositoryConfiguration.class))))
    public MappingJacksonValue getRepositoryConfigurations() {
        var result = new MappingJacksonValue(repositoryConfigurationService.getConfigurations());
        result.setSerializationView(getViewClass());
        return result;
    }

    @Operation(description = "msg.repository-settings.template.desc", summary = "msg.repository-settings.template.summary")
    @PostMapping(value = "/template", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @AdminPrivilege
    @ApiResponse(content = @Content(schema = @Schema(implementation = RepositoryConfiguration.class)))
    public MappingJacksonValue getConfigurationTemplate(@RequestBody @Valid CreateRepositoryTemplateModel request) {
        var result = new MappingJacksonValue(repositoryConfigurationService.initialize(request.getType()));
        result.setSerializationView(getViewClass());
        return result;
    }

    @Operation(description = "msg.repository-settings.validate.desc", summary = "msg.repository-settings.validate.summary")
    @PostMapping(value = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
    @AdminPrivilege
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validateConfiguration(@RequestBody @Valid CURepositoryConfigurationModel request) throws RepositoryValidationException, IOException {
        var configuration = repositoryConfigurationService.transform(request);
        repositoryConfigurationService.validate(configuration);
    }

    @Operation(description = "msg.repository-settings.create-or-update.desc", summary = "msg.repository-settings.create-or-update.summary")
    @PatchMapping(consumes = "application/merge-patch+json")
    @AdminPrivilege
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void createOrUpdateConfiguration(@RequestBody @Valid CURepositoryConfigurationModel request) throws IOException, RepositoryValidationException {
        var configuration = repositoryConfigurationService.transform(request);
        repositoryConfigurationService.validate(configuration);
        repositoryConfigurationService.store(configuration);
        settingsService.commit();
    }

    @Operation(description = "msg.repository-settings.delete.desc", summary = "msg.repository-settings.delete.summary")
    @DeleteMapping("/{repo-id}")
    @AdminPrivilege
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConfiguration(@PathVariable("repo-id") String id) throws IOException {
        repositoryConfigurationService.delete(id);
        settingsService.commit();
    }

    @Operation(description = "msg.repository-settings.revert.desc", summary = "msg.repository-settings.revert.summary")
    @DeleteMapping
    @AdminPrivilege
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revertConfiguration() throws IOException {
        repositoryConfigurationService.revert();
        settingsService.commit();
    }

    protected abstract Class<?> getViewClass();

}
