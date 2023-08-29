/* Copyright © 2023 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.*/
package org.openl.rules.rest.model;

import java.util.Optional;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.rest.model.converters.ProjectStatusDeserializer;
import org.openl.util.StringUtils;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Model for updating project status
 *
 * @author Vladyslav Pikus
 */
public class ProjectStatusUpdateModel {

    @JsonDeserialize(using = ProjectStatusDeserializer.class)
    private ProjectStatus status;

    private String branch;

    private String revision;

    private String comment;

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public Optional<String> getBranch() {
        return Optional.ofNullable(branch);
    }

    public void setBranch(String branch) {
        this.branch = StringUtils.trimToNull(branch);
    }

    public Optional<String>  getRevision() {
        return Optional.ofNullable(revision);
    }

    public void setRevision(String revision) {
        this.revision = StringUtils.trimToNull(revision);
    }

    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
