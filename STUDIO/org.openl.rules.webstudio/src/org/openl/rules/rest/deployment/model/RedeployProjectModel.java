package org.openl.rules.rest.deployment.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.studio.projects.model.ProjectIdModel;

@JsonDeserialize(builder = RedeployProjectModel.Builder.class)
@Schema(description = "Redeploy project request model")
public class RedeployProjectModel {

    @Parameter(description = "Project identifier to redeploy", required = true)
    public final ProjectIdModel projectId;

    @Parameter(description = "Redeployment reason comment")
    public final String comment;

    public RedeployProjectModel(Builder builder) {
        this.projectId = builder.projectId;
        this.comment = builder.comment;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private ProjectIdModel projectId;
        private String comment;

        public Builder projectId(ProjectIdModel projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public RedeployProjectModel build() {
            return new RedeployProjectModel(this);
        }
    }


}
