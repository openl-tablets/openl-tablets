package org.openl.studio.settings.model.repositories;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Parameter;

public class RepositoriesBatchRequest {

    @Parameter(description = "Design repositories")
    @Valid
    private RepositoriesBatchModel design;

    @Parameter(description = "Production repositories")
    @Valid
    private RepositoriesBatchModel production;

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

}
