package org.openl.studio.projects.model.merge;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What a merge changed in the target branch.
 *
 * <p>A file is compared before and after the merge by reading it at the two revisions on the target branch.
 */
@Schema(description = "What the merge changed in the target branch")
public record MergeSummaryView(
        @Parameter(description = "The branch merged in")
        String sourceBranch,

        @Parameter(description = "The branch the merge changed")
        String targetBranch,

        @Parameter(description = "Revision of the target branch before the merge")
        String before,

        @Parameter(description = "Revision of the target branch after the merge: the merge commit, or the commit of the descriptors a resolution wrote after it")
        String after,

        // An empty list is still told: the client reads it whatever the merge changed.
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @Parameter(description = "The projects the merge changed: the project asked for first, the others by name, the files outside any project last")
        List<ProjectChangesView> projects,

        @Parameter(description = "How many projects the user may not read the merge changed too")
        int hiddenProjects
) {
}
