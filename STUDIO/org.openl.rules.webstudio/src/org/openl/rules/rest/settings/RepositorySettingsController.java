package org.openl.rules.rest.settings;

import java.io.IOException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.rest.settings.model.RepositoriesBatchModel;
import org.openl.rules.rest.settings.model.RepositoriesBatchRequest;
import org.openl.rules.rest.settings.service.DeployConfigRepositorySettingsService;
import org.openl.rules.rest.settings.service.DesignRepositorySettingsService;
import org.openl.rules.rest.settings.service.ProductionRepositorySettingsService;
import org.openl.rules.rest.settings.service.RepositorySettingsService;
import org.openl.rules.security.AdminPrivilege;
import org.openl.rules.webstudio.web.admin.RepositoryValidationException;
import org.openl.rules.webstudio.web.admin.SettingsService;

@RestController
@RequestMapping("/admin/settings/repos")
@Tag(name = "Settings: Repositories")
@Validated
public class RepositorySettingsController {

    private final DesignRepositorySettingsService designRepositorySettingsService;
    private final ProductionRepositorySettingsService productionRepositorySettingsService;
    private final DeployConfigRepositorySettingsService deployConfigRepositorySettingsService;
    private final SettingsService settingsService;

    public RepositorySettingsController(DesignRepositorySettingsService designRepositorySettingsService,
                                        ProductionRepositorySettingsService productionRepositorySettingsService,
                                        DeployConfigRepositorySettingsService deployConfigRepositorySettingsService,
                                        SettingsService settingsService) {
        this.designRepositorySettingsService = designRepositorySettingsService;
        this.productionRepositorySettingsService = productionRepositorySettingsService;
        this.deployConfigRepositorySettingsService = deployConfigRepositorySettingsService;
        this.settingsService = settingsService;
    }

    @Operation(description = "msg.repository-settings.batch-update.desc", summary = "msg.repository-settings.batch-update.summary")
    @PostMapping(value = "/batch", consumes = MediaType.APPLICATION_JSON_VALUE)
    @AdminPrivilege
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void batchUpdate(@RequestBody RepositoriesBatchRequest request) throws IOException, RepositoryValidationException {
        processBatchRepository(request.getDesign(), designRepositorySettingsService);
        processBatchRepository(request.getProduction(), productionRepositorySettingsService);
        if (request.getDeployConfig() != null) {
            var configuration = deployConfigRepositorySettingsService.transform(request.getDeployConfig());
            deployConfigRepositorySettingsService.validate(configuration);
            deployConfigRepositorySettingsService.store(configuration);
        }
        settingsService.commit();
    }

    private void processBatchRepository(RepositoriesBatchModel request, RepositorySettingsService service) throws IOException, RepositoryValidationException {
        if (request == null) {
            return;
        }
        if (request.getDelete() != null) {
            for (String repo : request.getDelete()) {
                if (service.exists(repo)) {
                    service.delete(repo);
                }
            }
        }
        if (request.getCreateOrUpdate() != null) {
            for (var repo : request.getCreateOrUpdate()) {
                var configuration = service.transform(repo);
                service.validate(configuration);
                service.store(configuration);
            }
        }
    }

    @Operation(description = "msg.repository-settings.revert.desc", summary = "msg.repository-settings.revert.summary")
    @DeleteMapping
    @AdminPrivilege
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revertConfiguration() throws IOException {
        designRepositorySettingsService.revert();
        productionRepositorySettingsService.revert();
        deployConfigRepositorySettingsService.revert();
        settingsService.commit();
    }
}
