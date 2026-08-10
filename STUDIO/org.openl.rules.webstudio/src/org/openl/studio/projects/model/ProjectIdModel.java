package org.openl.studio.projects.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public class ProjectIdModel {

    private static final String ID_SEPARATOR = ":";

    @Getter
    private final String repository;
    @Getter
    private final String projectName;

    public ProjectIdModel(Builder builder) {
        this.repository = builder.repository;
        this.projectName = builder.projectName;
    }

    /**
     * Encodes this id into a single URL path segment, reading the name as UTF-8.
     *
     * <p>The alphabet is URL-safe: a standard Base64 id may carry a slash, which breaks the segment. See the
     * module {@code AGENTS.md}, section "Ids in a URL Path".
     */
    @JsonValue
    public String encode() {
        var src = repository + ID_SEPARATOR + projectName;
        return Base64.getUrlEncoder().encodeToString(src.getBytes(StandardCharsets.UTF_8));
    }

    @JsonCreator
    public static ProjectIdModel decode(String encoded) {
        // Both alphabets are accepted: ids issued before the URL-safe form became the default still live in
        // bookmarks and in legacy pages. A standard id never contains '-'/'_', so this mapping is a no-op for it.
        var normalized = encoded.replace('-', '+').replace('_', '/');
        var decoded = new String(Base64.getDecoder().decode(normalized), StandardCharsets.UTF_8);
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (ProjectIdModel) o;
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
