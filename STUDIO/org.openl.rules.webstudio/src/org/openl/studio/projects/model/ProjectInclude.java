package org.openl.studio.projects.model;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Optional project response expansion or listing behavior.")
public enum ProjectInclude {
    @Schema(description = "Include summary facet counts in the projects page response.")
    @JsonProperty("summary")
    SUMMARY,

    @Schema(description = "Include current compilation status details.")
    @JsonProperty("status")
    STATUS,

    @Schema(description = "Include deleted projects in project listings.")
    @JsonProperty("deleted")
    DELETED,

    @Schema(description = "Include resolved project modules and related descriptor fields.")
    @JsonProperty("modules")
    MODULES;

    public static List<ProjectInclude> normalize(Collection<ProjectInclude> includes) {
        if (includes == null || includes.isEmpty()) {
            return List.of();
        }
        var normalized = EnumSet.noneOf(ProjectInclude.class);
        includes.stream()
                .filter(Objects::nonNull)
                .forEach(normalized::add);
        return List.copyOf(normalized);
    }
}
