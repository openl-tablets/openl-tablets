package org.openl.studio.settings.model.repositories;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

public class RepositoriesBatchRequest {

    @Getter
    @Parameter(description = "Design repositories")
    @Setter
    @Valid
    private RepositoriesBatchModel design;

    @Getter
    @Parameter(description = "Production repositories")
    @Setter
    @Valid
    private RepositoriesBatchModel production;

}
