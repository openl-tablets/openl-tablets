package org.openl.studio.projects.model.merge;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Version of a conflicted file to retrieve.
 */
@Schema(description = "Version of a conflicted file")
public enum ConflictBase {

    /**
     * Base version: common ancestor before branches diverged.
     */
    BASE,

    /**
     * Ours version: from the current branch.
     */
    OURS,

    /**
     * Theirs version: from the merging branch.
     */
    THEIRS

}
