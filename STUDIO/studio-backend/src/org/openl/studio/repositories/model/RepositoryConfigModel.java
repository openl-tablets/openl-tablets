package org.openl.studio.repositories.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * Settings of a design repository the project forms need — how a new branch name is built, and the rules a
 * branch name and a commit comment must satisfy.
 *
 * <p>Only these public settings are exposed; credentials, URLs and the rest of the repository
 * administration stay behind the administration API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RepositoryConfigModel(
        @Parameter(description = "Configured branch. Absent when the repository has no branches")
        String branch,
        @Parameter(description = "Branch name glob patterns that are protected. Absent when none are configured")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<String> protectedBranches,
        @Parameter(description = "New branch rules. Absent when the repository has no branches")
        NewBranch newBranch,
        @Parameter(description = "Commit comment rules")
        Comment comment) {

    public RepositoryConfigModel(NewBranch newBranch, Comment comment) {
        this(null, null, newBranch, comment);
    }

    /** The configuration of a project that lives only in the workspace: nothing is suggested or required. */
    public static RepositoryConfigModel none() {
        return new RepositoryConfigModel(null, null, null,
                new Comment(null, null));
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

    /** How a commit comment is validated. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Comment(
            @Parameter(description = "Regular expression a comment must match. Absent when any comment is accepted")
            String userMessagePattern,
            @Parameter(description = "Message to show when a comment does not match the expression")
            String invalidUserMessageHint) {
    }
}
