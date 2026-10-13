package org.openl.studio.projects.model.merge;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What a merge would bring into the target branch, told before the merge.
 */
@Schema(description = "What the merge would bring into the target branch")
public record MergeScopeView(
        // An empty list is still told: the client reads it whatever the merge changed.
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @Parameter(description = "The projects the merge would change: the project asked for first, the others by name, the files outside any project last")
        List<ProjectScopeView> projects,

        @Parameter(description = "How many projects the user may not read the merge would change too")
        int hiddenProjects
) {
}
