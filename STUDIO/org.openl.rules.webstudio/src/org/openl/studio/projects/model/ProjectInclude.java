package org.openl.studio.projects.model;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Optional project response expansion or listing behavior.")
public enum ProjectInclude {
    @Schema(description = "Include summary facet counts in the projects page response.")
    SUMMARY,

    @Schema(description = "Include current compilation status details.")
    STATUS,

    @Schema(description = "Include deleted projects in project listings.")
    DELETED,

    @Schema(description = "Include resolved project modules and related descriptor fields.")
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
