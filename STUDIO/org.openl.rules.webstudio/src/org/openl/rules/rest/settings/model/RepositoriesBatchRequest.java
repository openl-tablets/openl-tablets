package org.openl.rules.rest.settings.model;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Parameter;

public class RepositoriesBatchRequest {

    @Parameter(description = "Design repositories")
    @Valid
    private RepositoriesBatchModel design;

    @Parameter(description = "Production repositories")
    @Valid
    private RepositoriesBatchModel production;

    @Parameter(description = "Deploy config")
    @Valid
    private DeployConfigRepositoryConfigurationModel deployConfig;

    public RepositoriesBatchModel getDesign() {
        return design;
    }

    public void setDesign(RepositoriesBatchModel design) {
        this.design = design;
    }

    public RepositoriesBatchModel getProduction() {
        return production;
    }

    public void setProduction(RepositoriesBatchModel production) {
        this.production = production;
    }

    public DeployConfigRepositoryConfigurationModel getDeployConfig() {
        return deployConfig;
    }

    public void setDeployConfig(DeployConfigRepositoryConfigurationModel deployConfig) {
        this.deployConfig = deployConfig;
    }
}
