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
 * match at the requested path comes first, followed by matches in each
 * enclosing directory in turn, ending at the repository root.
 *
 * <p>The {@code searchParents} flag controls the scope. When it is {@code false}
 * the list contains at most one entry — the match at the requested path. When it
 * is {@code true} the list also includes matches from enclosing directories up
 * to the repository root.
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
