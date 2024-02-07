package org.openl.rules.rest.model;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.rest.model.converters.ProjectStatusSerializer;
import org.openl.util.StringUtils;

public class ProjectViewModel {

    @Parameter(description = "Project Name", required = true)
    @JsonView(GenericView.Full.class)
    public final String name;

    @Parameter(description = "Author of latest update", required = true)
    @JsonView(GenericView.Full.class)
    public final String modifiedBy;

    @Parameter(description = "Date and time of latest update", required = true)
    @JsonView(GenericView.Full.class)
    public final ZonedDateTime modifiedAt;

    @Parameter(description = "Branch Name. Can be absent if current repository doesn't support branches")
    @JsonView({GenericView.CreateOrUpdate.class, GenericView.Full.class})
    public final String branch;

    @Parameter(description = "Revision ID", required = true)
    @JsonView({GenericView.CreateOrUpdate.class, GenericView.Full.class})
    public final String revision;

    @Parameter(description = "Project path in target repository. Can be absent if Design Repository is flat")
    @JsonView(GenericView.Full.class)
    public final String path;

    @Parameter(description = "Project identifier", required = true)
    @JsonView(GenericView.Full.class)
    public final String id;

    @Parameter(description = "Project Status", schema = @Schema(allowableValues = {"LOCAL",
            "ARCHIVED",
            "OPENED",
            "VIEWING_VERSION",
            "EDITING",
            "CLOSED"}))
    @JsonSerialize(using = ProjectStatusSerializer.class)
    @JsonView(GenericView.Full.class)
    public final ProjectStatus status;

    @Parameter(description = "Project Tags")
    @JsonView(GenericView.Full.class)
    public final Map<String, String> tags;

    @Parameter(description = "Project Comment")
    @JsonView(GenericView.Full.class)
    public final String comment;

    @Parameter(description = "Source Repository")
    @JsonView(GenericView.Full.class)
    public final String repository;

    private ProjectViewModel(Builder from) {
        this.name = from.name;
        this.modifiedBy = from.modifiedBy;
        this.modifiedAt = from.modifiedAt;
        this.branch = from.branch;
        this.revision = from.revision;
        this.path = from.path;
        this.id = from.id;
        this.status = from.status;
        this.tags = new TreeMap<>(from.tags);
        this.comment = from.comment;
        this.repository = from.repository;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String modifiedBy;
        private ZonedDateTime modifiedAt;
        private String branch;
        private String revision;
        private String path;
        public String id;
        public ProjectStatus status;
        private final Map<String, String> tags = new HashMap<>();
        private String comment;
        private String repository;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder modifiedBy(String modifiedBy) {
            this.modifiedBy = modifiedBy;
            return this;
        }

        public Builder modifiedAt(ZonedDateTime modifiedAt) {
            this.modifiedAt = modifiedAt;
            return this;
        }

        public Builder branch(String branch) {
            this.branch = branch;
            return this;
        }

        public Builder revision(String revision) {
            this.revision = revision;
            return this;
        }

        public Builder path(String path) {
            this.path = StringUtils.isEmpty(path) ? null : path;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder status(ProjectStatus status) {
            this.status = status;
            return this;
        }

        public Builder addTag(String name, String value) {
            this.tags.put(name, value);
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder repository(String repository) {
            this.repository = repository;
            return this;
        }

        public ProjectViewModel build() {
            return new ProjectViewModel(this);
        }
    }
}
