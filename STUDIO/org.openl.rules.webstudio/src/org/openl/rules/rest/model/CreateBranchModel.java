package org.openl.rules.rest.model;

import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Create branch model.
 *
 * @author Vladyslav Pikus
 */
public class CreateBranchModel {

    @NotNull
    private String branch;

    @Parameter(description = "Revision to branch from. Allows to branch from specific revision, tag or another branch. If not specified, HEAD revision will be used.")
    private String revision;

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }
}
