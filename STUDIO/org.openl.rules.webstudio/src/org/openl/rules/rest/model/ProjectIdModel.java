package org.openl.rules.rest.model;

import java.util.Base64;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class ProjectIdModel {

    private static final String ID_SEPARATOR = ":";

    private final String repository;
    private final String projectName;

    public ProjectIdModel(Builder builder) {
        this.repository = builder.repository;
        this.projectName = builder.projectName;
    }

    @JsonValue
    public String encode() {
        String src = repository + ID_SEPARATOR + projectName;
        return Base64.getEncoder().encodeToString(src.getBytes());
    }

    @JsonCreator
    public static ProjectIdModel decode(String encoded) {
        String decoded = new String(Base64.getDecoder().decode(encoded));
        var parts = decoded.indexOf(ID_SEPARATOR);
        if (parts == -1) {
            throw new IllegalArgumentException("Invalid projectId: " + encoded);
        }
        var repoId = decoded.substring(0, parts);
        var projectName = decoded.substring(parts + 1);
        return builder()
                .repository(repoId)
                .projectName(projectName)
                .build();
    }

    public String getRepository() {
        return repository;
    }

    public String getProjectName() {
        return projectName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProjectIdModel that = (ProjectIdModel) o;
        return Objects.equals(repository, that.repository) && Objects.equals(projectName, that.projectName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repository, projectName);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String repository;
        private String projectName;

        private Builder() {
        }

        public Builder repository(String repository) {
            this.repository = repository;
            return this;
        }

        public Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public ProjectIdModel build() {
            return new ProjectIdModel(this);
        }
    }


}
