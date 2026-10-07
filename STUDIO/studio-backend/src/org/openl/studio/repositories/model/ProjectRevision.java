package org.openl.studio.repositories.model;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

import org.openl.rules.rest.model.UserInfoModel;

/**
 * One revision of what was asked about: a project, or a single file of it when the history is read per file.
 *
 * <p>The two differ only in what the revision is measured against — the same commit is a technical revision
 * for a file it did not touch and a real one for the project around it.
 */
@Builder
public record ProjectRevision(
        @Parameter(description = "Revision number", required = true)
        String revisionNo,

        @Parameter(description = "Short revision number")
        String shortRevisionNo,

        @Parameter(description = "Creation date-time", required = true)
        ZonedDateTime createdAt,

        @Parameter(description = "Full comment", required = true)
        String fullComment,

        @Parameter(description = "Author")
        @JsonView({UserInfoModel.View.Short.class})
        UserInfoModel author,

        @Parameter(description = "Whether this revision removed what was asked about — the project, or the file when the history is read per file.", required = true)
        boolean deleted,

        @Parameter(description = "Whether this revision left what was asked about unchanged — the project, or the file when the history is read per file.", required = true)
        boolean technicalRevision,

        @Parameter(description = "Comment parts. Always has 3 parts if present.")
        List<String> commentParts) {
}
