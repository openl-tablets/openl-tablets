package org.openl.studio.deployment.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.ai.tool.annotation.ToolParam;

import org.openl.rules.rest.validation.ProjectNameConstraint;
import org.openl.studio.projects.model.ProjectIdModel;

@JsonDeserialize(builder = DeployProjectModel.Builder.class)
@Schema(description = "Deploy project request model")
public class DeployProjectModel {

    private static final String FIELD_PROD_REPO_ID_DESC = "Production repository id";
    private static final String FIELD_DEPLOYMENT_NAME_DESC = "Deployment name";
    private static final String FIELD_PROJECT_ID_DESC = "Project identifier to deploy";
    private static final String FIELD_DEPLOYMENT_COMMENT_DESC = "Deployment reason comment";

    @Parameter(description = FIELD_PROD_REPO_ID_DESC, required = true)
    @ToolParam(description = FIELD_PROD_REPO_ID_DESC)
    public final String productionRepositoryId;

    @Parameter(description = FIELD_DEPLOYMENT_NAME_DESC, required = true)
    @ToolParam(description = FIELD_DEPLOYMENT_NAME_DESC)
    @ProjectNameConstraint
    public final String deploymentName;

    @Parameter(description = FIELD_PROJECT_ID_DESC, required = true)
    @ToolParam(description = FIELD_PROJECT_ID_DESC)
    public final ProjectIdModel projectId;

    @Parameter(description = FIELD_DEPLOYMENT_COMMENT_DESC)
    @ToolParam(description = FIELD_DEPLOYMENT_COMMENT_DESC)
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
