package org.openl.studio.transfer.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import org.jspecify.annotations.Nullable;

import org.openl.studio.projects.model.merge.ConflictBase;
import org.openl.studio.projects.service.files.ConflictPolicy;

/**
 * The operation a transfer link is asked for.
 *
 * @author Yury Molchan
 */
@Builder
@Jacksonized
@Schema(description = "The operation a transfer link is asked for")
public record TransferLinkRequest(
        @Parameter(description = "What the link moves.", required = true)
        @NotNull TransferKind kind,

        @Parameter(description = "Id of the project.", required = true)
        @NotBlank String projectId,

        @Parameter(description = """
                Project-relative path of the file, of the folder of an archive (empty for the \
                root), or of the conflicted file of a conflict version.""", required = true)
        @NotNull String path,

        @Parameter(description = "Branch the project must be on; omitted for the branch it is on.")
        @Nullable String branch,

        @Parameter(description = "Revision to download; omitted for the working copy. Downloads only.")
        @Nullable String version,

        @Parameter(description = "Version of the conflicted file. Conflict versions only.")
        @Nullable ConflictBase side,

        @Parameter(description = "What an upload does with a file already at the path; FAIL by default.")
        @Nullable ConflictPolicy conflictPolicy,

        @Parameter(description = """
                Version of the file an upload replaces, as a read of the file answered it in its \
                ETag: the upload replaces the file only while it has that version, and writes nothing when it \
                changed since. Uploads only; the conflict policy is not used with it.""")
        @Nullable String expectedVersion,

        @Parameter(description = "Whether an upload creates missing parent folders; true by default.")
        @Nullable Boolean createFolders,

        @Parameter(description = "Size in bytes the uploaded file must have. Uploads only.")
        @PositiveOrZero @Nullable Long size,

        @Parameter(description = "Hex SHA-256 the uploaded file must have. Uploads only.")
        @Pattern(regexp = "[0-9a-fA-F]{64}") @Nullable String sha256) {
}
