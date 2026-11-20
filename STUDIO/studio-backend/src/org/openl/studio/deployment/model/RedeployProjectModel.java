package org.openl.studio.deployment.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.rules.workspace.ProjectId;

@JsonDeserialize(builder = RedeployProjectModel.Builder.class)
@Schema(description = "Redeploy project request model")
public class RedeployProjectModel {

    private static final String FIELD_PROJECT_ID_DESC = "Project identifier to redeploy";
    private static final String FIELD_DEPLOYMENT_COMMENT_DESC = "Redeployment reason comment";
    private static final String FIELD_DEPENDENCY_SOURCE_DESC = "Where a dependency project both a design "
            + "repository and the Maven repository have is taken from: REPOSITORY, the default, or MAVEN";

    @Parameter(description = FIELD_PROJECT_ID_DESC, required = true)
    public final ProjectId projectId;

    @Parameter(description = FIELD_DEPLOYMENT_COMMENT_DESC)
    public final String comment;

    @Parameter(description = FIELD_DEPENDENCY_SOURCE_DESC)
    public final DependencySource dependencySource;

    public RedeployProjectModel(Builder builder) {
        this.projectId = builder.projectId;
        this.comment = builder.comment;
        this.dependencySource = builder.dependencySource;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private ProjectId projectId;
        private String comment;
        private DependencySource dependencySource;

        public Builder projectId(ProjectId projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder dependencySource(DependencySource dependencySource) {
            this.dependencySource = dependencySource;
            return this;
        }

        public RedeployProjectModel build() {
            return new RedeployProjectModel(this);
        }
    }


}
