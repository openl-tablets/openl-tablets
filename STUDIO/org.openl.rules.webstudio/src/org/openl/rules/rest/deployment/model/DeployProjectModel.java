package org.openl.rules.rest.deployment.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.rules.rest.validation.ProjectNameConstraint;
import org.openl.studio.projects.model.ProjectIdModel;

@JsonDeserialize(builder = DeployProjectModel.Builder.class)
@Schema(description = "Deploy project request model")
public class DeployProjectModel {

    @Parameter(description = "Production repository id", required = true)
    public final String productionRepositoryId;

    @Parameter(description = "Deployment name", required = true)
    @ProjectNameConstraint
    public final String deploymentName;

    @Parameter(description = "Project identifier to deploy", required = true)
    public final ProjectIdModel projectId;

    @Parameter(description = "Deployment reason comment")
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
