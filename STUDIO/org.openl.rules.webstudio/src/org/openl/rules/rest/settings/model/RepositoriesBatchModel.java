/* Copyright © 2025 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package org.openl.rules.rest.settings.model;

import java.util.List;
import javax.validation.Valid;

import io.swagger.v3.oas.annotations.Parameter;

public class RepositoriesBatchModel {

    @Parameter(description = "List of repository settings to create or update")
    @Valid
    private List<CURepositoryConfigurationModel> createOrUpdate;

    @Parameter(description = "List of repository IDs to delete")
    private List<String> delete;

    public List<CURepositoryConfigurationModel> getCreateOrUpdate() {
        return createOrUpdate;
    }

    public void setCreateOrUpdate(List<CURepositoryConfigurationModel> createOrUpdate) {
        this.createOrUpdate = createOrUpdate;
    }

    public List<String> getDelete() {
        return delete;
    }

    public void setDelete(List<String> delete) {
        this.delete = delete;
    }
}
