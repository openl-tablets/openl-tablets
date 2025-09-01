package org.openl.rules.rest.deployment.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import org.openl.rules.rest.model.ProjectIdModel;

@JsonDeserialize(builder = DeployProjectModel.Builder.class)
public class DeployProjectModel {

    public final String productionRepositoryId;
    public final String deploymentName;
    public final ProjectIdModel projectId;
    public final String comment;

    public DeployProjectModel(Builder builder) {
        this.productionRepositoryId = builder.productionRepositoryId;
        this.deploymentName = builder.deploymentName;
        this.projectId = builder.projectId;
        this.comment = builder.comment;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String productionRepositoryId;
        private String deploymentName;
        private ProjectIdModel projectId;
        private String comment;

        public Builder productionRepositoryId(String productionRepositoryId) {
            this.productionRepositoryId = productionRepositoryId;
            return this;
        }

        public Builder deploymentName(String deploymentName) {
            this.deploymentName = deploymentName;
            return this;
        }

        public Builder projectId(ProjectIdModel projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public DeployProjectModel build() {
            return new DeployProjectModel(this);
        }
    }

}
