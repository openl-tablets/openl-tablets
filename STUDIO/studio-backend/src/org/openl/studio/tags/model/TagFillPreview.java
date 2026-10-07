package org.openl.studio.tags.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * What filling tags from the project name templates would do to one project, before anything is written.
 *
 * @param projectName the project's business name, as the templates match it
 * @param modifiable  whether the project can be changed by the current user, which is when it has no blocker
 * @param blocker     why the project cannot be changed now, absent when it can
 * @param tags        one entry per tag type the template derived a value for
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "What filling tags from the project name templates would do to one project")
public record TagFillPreview(
        @Parameter(description = "Project business name")
        String projectName,
        @Parameter(description = "Whether the current user can change this project")
        boolean modifiable,
        @Parameter(description = "Why the current user cannot change this project now")
        @Nullable TagFillBlocker blocker,
        @Parameter(description = "What happens to each tag the templates derived")
        List<TagFillItem> tags) {

    /** The preview of a project that can be changed now exactly when nothing blocks it. */
    public static TagFillPreview of(String projectName, @Nullable TagFillBlocker blocker, List<TagFillItem> tags) {
        return new TagFillPreview(projectName, blocker == null, blocker, tags);
    }

    /**
     * One tag of the project: the value it carries now, the value the template derived, and what happens
     * to it.
     *
     * @param type    tag type name
     * @param current the value the project carries now, absent when it has none
     * @param derived the value the template derived
     * @param state   what filling does with the derived value
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "What filling does with one tag of a project")
    public record TagFillItem(
            @Parameter(description = "Tag type name")
            String type,
            @Parameter(description = "The value the project carries now")
            @Nullable String current,
            @Parameter(description = "The value the project name template derived")
            String derived,
            @Parameter(description = "What happens to the derived value")
            TagFillState state) {
    }
}
