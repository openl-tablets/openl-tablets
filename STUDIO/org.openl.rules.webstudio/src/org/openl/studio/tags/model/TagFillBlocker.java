package org.openl.studio.tags.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * Why filling tags from the project name templates cannot write the tags of a project now.
 *
 * <p>Each reason tells the user what to do before filling the project again.
 *
 * @param reason   why the project cannot take its tags now
 * @param lockedBy who holds the lock of a locked project, absent when that is unknown
 * @param branch   the protected branch of the project
 *
 * @author Yury Molchan
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Why a project cannot take its tags now")
public record TagFillBlocker(
        @Parameter(description = "Why the project cannot take its tags now")
        Reason reason,
        @Parameter(description = "Who holds the lock of the project, when it is locked")
        @Nullable String lockedBy,
        @Parameter(description = "The protected branch of the project, when its branch is protected")
        @Nullable String branch) {

    /** A reason that names nobody and no branch. */
    public static TagFillBlocker of(Reason reason) {
        return new TagFillBlocker(reason, null, null);
    }

    /** The project is locked by the given user, or by someone unknown. */
    public static TagFillBlocker locked(@Nullable String lockedBy) {
        return new TagFillBlocker(Reason.LOCKED, lockedBy == null || lockedBy.isBlank() ? null : lockedBy, null);
    }

    /** The given branch of the project is protected. */
    public static TagFillBlocker branchProtected(@Nullable String branch) {
        return new TagFillBlocker(Reason.BRANCH_PROTECTED, null, branch);
    }

    /**
     * Why a project cannot take its tags now.
     */
    @Schema(description = "Why a project cannot take its tags now")
    public enum Reason {

        /**
         * Another user holds the lock of the project, usually because they are editing it. It takes its tags once
         * the lock is released.
         */
        @Schema(description = "The project is locked")
        @JsonProperty("locked")
        LOCKED,

        /**
         * The current user holds a lock on the project that outlived their editing, for example after a change that
         * failed. It takes its tags once the user opens and closes the project, which releases the lock.
         */
        @Schema(description = "The current user holds a lock on the project that outlived their editing")
        @JsonProperty("lockedByYou")
        LOCKED_BY_YOU,

        /**
         * The branch of the project is protected, so its changes go through a merge.
         */
        @Schema(description = "The branch of the project is protected")
        @JsonProperty("branchProtected")
        BRANCH_PROTECTED,

        /**
         * The current user may not write to the project.
         */
        @Schema(description = "The current user may not change the project")
        @JsonProperty("noPermission")
        NO_PERMISSION,

        /**
         * The current user opened an older revision of the project to read it, and the first write into it is
         * theirs to decide.
         */
        @Schema(description = "The current user opened an older revision of the project")
        @JsonProperty("olderRevision")
        OLDER_REVISION,

        /**
         * The project is closed, and its repository keeps projects as archives, so it takes its tags once opened.
         */
        @Schema(description = "The project is closed in a repository that keeps projects as archives")
        @JsonProperty("archive")
        ARCHIVE,
    }
}
