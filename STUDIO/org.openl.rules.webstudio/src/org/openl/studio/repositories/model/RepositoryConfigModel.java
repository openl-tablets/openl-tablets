package org.openl.studio.repositories.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * Settings of a design repository the project forms need — how a new branch name and a commit comment are
 * built, and the rules they must satisfy.
 *
 * <p>Only these public settings are exposed; credentials, URLs and the rest of the repository
 * administration stay behind the administration API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RepositoryConfigModel(
        @Parameter(description = "Configured branch. Absent when the repository has no branches")
        String branch,
        @Parameter(description = "New branch rules. Absent when the repository has no branches")
        NewBranch newBranch,
        @Parameter(description = "Commit comment rules")
        Comment comment) {

    public RepositoryConfigModel(NewBranch newBranch, Comment comment) {
        this(null, newBranch, comment);
    }

    /** The configuration of a project that lives only in the workspace: nothing is suggested or required. */
    public static RepositoryConfigModel none() {
        return new RepositoryConfigModel(null, null, new Comment(null, null, new Templates(null, null, null, null)));
    }

    /**
     * How a branch name is suggested and validated.
     *
     * <p>The pattern accepts the {@code {project-name}}, {@code {username}} and {@code {current-date}}
     * placeholders.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record NewBranch(
            @Parameter(description = "Pattern the suggested branch name is built from")
            String pattern,
            @Parameter(description = "Regular expression a branch name must match. Absent when any name is accepted")
            String namePattern,
            @Parameter(description = "Message to show when a branch name does not match the expression")
            String invalidNameHint) {
    }

    /**
     * How a commit comment is suggested and validated.
     *
     * <p>Every template accepts the {@code {project-name}} placeholder; the restore one also accepts
     * {@code {revision}}, {@code {author}} and {@code {datetime}}.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Comment(
            @Parameter(description = "Regular expression a comment must match. Absent when the repository does not customize comments")
            String userMessagePattern,
            @Parameter(description = "Message to show when a comment does not match the expression")
            String invalidUserMessageHint,
            @Parameter(description = "Templates the suggested comments are built from")
            Templates templates) {
    }

    /** The comment template of every action that commits. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Templates(
            @Parameter(description = "Template for saving a project")
            String save,
            @Parameter(description = "Template for creating a project")
            String create,
            @Parameter(description = "Template for copying a project")
            String copy,
            @Parameter(description = "Template for restoring a project from an old revision")
            String restoreFrom) {
    }
}
