package org.openl.studio.projects.model.resources;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * Response of the project file lookup API.
 *
 * <p>The {@code files} list is ordered from nearest to farthest ancestor: the
 * project root match comes first, then each parent directory in turn, ending
 * with the repository root.
 *
 * <p>For {@code scope=exact} the list contains at most one entry.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result of the project file lookup")
public record ProjectFileLookupResponse(

        @Schema(description = "Matching files ordered from nearest to farthest ancestor")
        List<ProjectFileMatch> files
) {

    /**
     * A single matching file in the lookup result.
     *
     * @param path    repository-relative path of the matching file
     * @param content raw file content (UTF-8); present only when {@code includeContent=true}
     */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "A single matching file in the lookup result")
    public record ProjectFileMatch(

            @Schema(description = "Repository-relative path of the matching file")
            String path,

            @Schema(description = "Raw file content (UTF-8). Present only when includeContent=true")
            @Nullable String content
    ) {
    }
}
