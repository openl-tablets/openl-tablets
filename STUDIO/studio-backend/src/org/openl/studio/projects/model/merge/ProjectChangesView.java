package org.openl.studio.projects.model.merge;

import java.util.List;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

import org.openl.rules.workspace.ProjectId;
import org.openl.studio.projects.model.project.status.ChangeType;

/**
 * A project a merge changed, with the files it changed.
 *
 * <p>The files outside any project form a group of their own, which names no project.
 */
@Builder
@Schema(description = "A project the merge changed, or the files outside any project")
public record ProjectChangesView(
        @Parameter(description = "Id of the project; absent for the files outside any project")
        @Nullable ProjectId id,

        @Parameter(description = "Name of the project; absent for the files outside any project")
        @Nullable String name,

        @Parameter(description = "Path of the project folder from the root of the repository; absent for the files outside any project")
        @Nullable String path,

        @Parameter(description = "Whether it is the project the merge was asked for; every other project was changed by the same merge")
        boolean current,

        @Parameter(description = "What the merge did to the project as a whole")
        @Nullable ChangeType status,

        @Parameter(description = "The files the merge changed, workbooks first")
        List<FileChangeView> files
) {
}
