package org.openl.studio.projects.model.merge;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.rules.workspace.ProjectId;
import org.openl.studio.projects.model.project.status.ChangeType;

/**
 * A project a merge would change, told before the merge.
 *
 * <p>The files outside any project form a group of their own, which names no project.
 */
@Builder
@Schema(description = "A project the merge would change, or the files outside any project")
public record ProjectScopeView(
        @Parameter(description = "Id of the project; absent for the files outside any project")
        @Nullable ProjectId id,

        @Parameter(description = "Name of the project; absent for the files outside any project")
        @Nullable String name,

        @Parameter(description = "Path of the project folder from the root of the repository; absent for the files outside any project")
        @Nullable String path,

        @Parameter(description = "Whether it is the project the merge was asked for; every other project is changed by the same merge")
        boolean current,

        @Parameter(description = "What the merge does to the project as a whole")
        @Nullable ChangeType status,

        @Parameter(description = "How many of its files the merge changes")
        int changedFiles,

        @Parameter(description = "Paths of its files both branches changed: the merge combines them or reports them as conflicts")
        List<String> changedInBoth
) {
}
