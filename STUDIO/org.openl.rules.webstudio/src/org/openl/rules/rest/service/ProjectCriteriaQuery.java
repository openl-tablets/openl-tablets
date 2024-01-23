package org.openl.rules.rest.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.openl.rules.project.abstraction.ProjectStatus;

/**
 * Project criteria query. Used to filter projects in {@link ProjectService}.
 *
 * @author Vladyslav Pikus
 */
public class ProjectCriteriaQuery {

    private final String repositoryId;
    private final ProjectStatus status;
    private final Map<String, String> tags;

    private ProjectCriteriaQuery(Builder builder) {
        this.repositoryId = builder.repositoryId;
        this.status = builder.status;
        this.tags = Map.copyOf(builder.tags);
    }

    public Optional<String> getRepositoryId() {
        return Optional.ofNullable(repositoryId);
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public Optional<ProjectStatus> getStatus() {
        return Optional.ofNullable(status);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String repositoryId;
        private ProjectStatus status;
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

        public Builder tag(String type, String name) {
            tags.put(type, name);
            return this;
        }

        public ProjectCriteriaQuery build() {
            return new ProjectCriteriaQuery(this);
        }
    }
}
