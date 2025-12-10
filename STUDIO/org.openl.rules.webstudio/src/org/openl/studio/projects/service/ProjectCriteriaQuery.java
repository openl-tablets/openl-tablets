package org.openl.studio.projects.service;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.studio.projects.model.ProjectIdModel;

/**
 * Project criteria query. Used to filter projects in {@link ProjectService}.
 *
 * @author Vladyslav Pikus
 */
@JsonDeserialize(builder = ProjectCriteriaQuery.Builder.class)
public record ProjectCriteriaQuery(
        @Schema(description = "Identifier of the repository to filter projects by.")
        String repositoryId,

        @Schema(description = "Status of the projects to filter by.")
        ProjectStatus status,

        @Schema(description = "Identifier of the project that the returned projects depend on.")
        ProjectIdModel dependsOn,

        @Schema(description = "Set of tags to filter projects by.")
        Map<String, String> tags
) {

    private ProjectCriteriaQuery(Builder builder) {
        this(builder.repositoryId,
                builder.status,
                builder.dependsOn,
                Map.copyOf(builder.tags));
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private String repositoryId;
        private ProjectStatus status;
        private ProjectIdModel dependsOn;
        private final Map<String, String> tags = new HashMap<>();

        private Builder() {
        }

        public Builder repositoryId(String repositoryId) {
            this.repositoryId = repositoryId;
            return this;
        }

        public Builder status(ProjectStatus status) {
            this.status = status;
            return this;
        }

        public Builder dependsOn(ProjectIdModel dependsOn) {
            this.dependsOn = dependsOn;
            return this;
        }

        public Builder tag(String type, String name) {
            tags.put(type, name);
            return this;
        }

        public ProjectCriteriaQuery build() {
            return new ProjectCriteriaQuery(this);
        }
    }
}
