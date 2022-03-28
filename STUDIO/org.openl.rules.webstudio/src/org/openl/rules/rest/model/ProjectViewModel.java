package org.openl.rules.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.ZonedDateTime;
import java.util.Date;

public class ProjectViewModel {

    @Schema(description = "Project Name", required = true)
    public final String name;

    @Schema(description = "Author of latest update", required = true)
    public final String modifiedBy;

    @Schema(description = "Date and time of latest update", required = true)
    public final ZonedDateTime modifiedAt;

    @Schema(description = "Branch Name. Can be absent if current repository doesn't support branches")
    public final String branch;

    @Schema(description = "Revision ID", required = true)
    public final String rev;

    @Schema(description = "Project path in target repository. Can be absent if Design Repository is flat")
    public final String path;

    private ProjectViewModel(Builder from) {
        this.name = from.name;
        this.modifiedBy = from.modifiedBy;
        this.modifiedAt = from.modifiedAt;
        this.branch = from.branch;
        this.rev = from.rev;
        this.path = from.path;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String modifiedBy;
        private ZonedDateTime modifiedAt;
        private String branch;
        private String rev;
        private String path;

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

        public Builder rev(String rev) {
            this.rev = rev;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public ProjectViewModel build() {
            return new ProjectViewModel(this);
        }
    }
}
