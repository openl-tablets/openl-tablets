package org.openl.rules.rest.deployment.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import org.openl.rules.rest.model.ProjectIdModel;

@JsonDeserialize(builder = RedeployProjectModel.Builder.class)
public class RedeployProjectModel {

    public final ProjectIdModel projectId;
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
