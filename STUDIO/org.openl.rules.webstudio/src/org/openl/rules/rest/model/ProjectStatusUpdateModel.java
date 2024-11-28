package org.openl.rules.rest.model;

import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.rest.model.converters.ProjectStatusDeserializer;
import org.openl.util.StringUtils;

/**
 * Model for updating project status
 *
 * @author Vladyslav Pikus
 */
public class ProjectStatusUpdateModel {

    @Parameter(description = "Project Status", schema = @Schema(allowableValues = {"OPENED", "CLOSED"}))
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

    public Optional<String> getRevision() {
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
