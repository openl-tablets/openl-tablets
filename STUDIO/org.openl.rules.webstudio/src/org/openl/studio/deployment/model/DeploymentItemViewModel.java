package org.openl.studio.deployment.model;

import java.time.ZonedDateTime;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Deployment item view model")
public class DeploymentItemViewModel {

    @Parameter(description = "Project name", required = true)
    public final String name;

    @Parameter(description = "Author of latest deploy", required = true)
    public final String modifiedBy;

    @Parameter(description = "Date and time of latest deploy", required = true)
    public final ZonedDateTime modifiedAt;

    @Parameter(description = "Deployed project revision", required = true)
    public final String revision;

    public DeploymentItemViewModel(Builder from) {
        this.name = from.name;
        this.modifiedBy = from.modifiedBy;
        this.modifiedAt = from.modifiedAt;
        this.revision = from.revision;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String modifiedBy;
        private ZonedDateTime modifiedAt;
        private String revision;

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

        public Builder revision(String revision) {
            this.revision = revision;
            return this;
        }

        public DeploymentItemViewModel build() {
            return new DeploymentItemViewModel(this);
        }
    }

}
