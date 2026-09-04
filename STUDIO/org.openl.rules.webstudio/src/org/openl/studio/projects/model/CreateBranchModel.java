package org.openl.studio.projects.model;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;

/**
 * Create branch model.
 *
 * @author Vladyslav Pikus
 */
public class CreateBranchModel {

    @Getter
    @NotNull
    @Setter
    private String branch;

    @Getter
    @Parameter(description = "Revision to branch from. Allows to branch from specific revision, tag or another branch. If not specified, HEAD revision will be used.")
    @Setter
    private String revision;
}
