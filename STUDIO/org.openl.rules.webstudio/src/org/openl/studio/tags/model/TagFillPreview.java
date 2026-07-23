package org.openl.studio.tags.model;

import java.util.List;
import jakarta.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What filling tags from the project name templates would do to one project, before anything is written.
 *
 * @param projectName the project's business name, as the templates match it
 * @param modifiable  whether the project can be changed by the current user
 * @param tags        one entry per tag type the template derived a value for
 */
@Schema(description = "What filling tags from the project name templates would do to one project")
public record TagFillPreview(
        @Schema(description = "Project business name")
        String projectName,
        @Schema(description = "Whether the current user can change this project")
        boolean modifiable,
        @Schema(description = "What happens to each tag the templates derived")
        List<TagFillItem> tags) {

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
            @Schema(description = "Tag type name")
            String type,
            @Schema(description = "The value the project carries now")
            @Nullable String current,
            @Schema(description = "The value the project name template derived")
            String derived,
            @Schema(description = "What happens to the derived value")
            TagFillState state) {
    }
}
