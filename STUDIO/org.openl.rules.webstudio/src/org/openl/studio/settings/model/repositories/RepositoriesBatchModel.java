package org.openl.studio.settings.model.repositories;

import java.util.List;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

public class RepositoriesBatchModel {

    @Getter
    @Parameter(description = "List of repository settings to create or update")
    @Setter
    @Valid
    private List<CURepositoryConfigurationModel> createOrUpdate;

    @Getter
    @Parameter(description = "List of repository IDs to delete")
    @Setter
    private List<String> delete;
}
