package org.openl.studio.transfer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What a transfer link moves.
 *
 * @author Yury Molchan
 */
@Schema(description = "What a transfer link moves")
public enum TransferKind {

    /** A file of a project, from the working copy or a revision, to the client. */
    @JsonProperty("download")
    DOWNLOAD,

    /** A folder of a project, as a ZIP archive, to the client. */
    @JsonProperty("archive")
    ARCHIVE,

    /** One version of a file of the merge conflict the client has pending, to the client. */
    @JsonProperty("conflictVersion")
    CONFLICT_VERSION,

    /** A file from the client into the working copy of a project. */
    @JsonProperty("upload")
    UPLOAD
}
