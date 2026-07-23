package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class RepositoryConfigServiceTest {

    private final MockEnvironment environment = new MockEnvironment();
    private final RepositoryConfigService service = new RepositoryConfigService(environment);

    @Test
    void gitRepositoryExposesItsBranchRules() {
        environment.setProperty("repository.design.factory", "repo-git");
        environment.setProperty("repository.design.new-branch.pattern", "{project-name}/{username}/{current-date}");
        environment.setProperty("repository.design.new-branch.regex", "[A-Za-z0-9/-]+");
        environment.setProperty("repository.design.new-branch.regex-error", "Letters, digits and dashes only");

        var newBranch = service.getConfig("design").newBranch();

        assertEquals("{project-name}/{username}/{current-date}", newBranch.pattern());
        assertEquals("[A-Za-z0-9/-]+", newBranch.namePattern());
        assertEquals("Letters, digits and dashes only", newBranch.invalidNameHint());
    }

    @Test
    void repositoryWithoutBranchesHasNoBranchRules() {
        environment.setProperty("repository.design.factory", "repo-jdbc");

        assertNull(service.getConfig("design").newBranch());
    }

    @Test
    void commentTemplatesAreExposedForEveryActionThatCommits() {
        environment.setProperty("repository.design.factory", "repo-git");
        environment.setProperty("repository.design.comment-template.user-message.default.save", "Project {project-name} is saved.");
        environment.setProperty("repository.design.comment-template.user-message.default.create", "Project {project-name} is created.");
        environment.setProperty("repository.design.comment-template.user-message.default.copied-from", "Copied from: {project-name}.");
        environment.setProperty("repository.design.comment-template.user-message.default.restored-from", "Restored from revision of {author} on {datetime}.");

        var templates = service.getConfig("design").comment().templates();

        assertEquals("Project {project-name} is saved.", templates.save());
        assertEquals("Project {project-name} is created.", templates.create());
        assertEquals("Copied from: {project-name}.", templates.copy());
        assertEquals("Restored from revision of {author} on {datetime}.", templates.restoreFrom());
    }

    @Test
    void commentExpressionIsExposedOnlyWhileCommentsAreCustomized() {
        environment.setProperty("repository.design.factory", "repo-git");
        environment.setProperty("repository.design.comment-template.use-custom-comments", "true");
        environment.setProperty("repository.design.comment-template.comment-validation-pattern", "EPBDS-\\d+.*");
        environment.setProperty("repository.design.comment-template.invalid-comment-message", "Start the comment with a ticket");

        var comment = service.getConfig("design").comment();

        assertEquals("EPBDS-\\d+.*", comment.userMessagePattern());
        assertEquals("Start the comment with a ticket", comment.invalidUserMessageHint());
    }

    @Test
    void commentExpressionIsHiddenWhenCommentsAreNotCustomized() {
        environment.setProperty("repository.design.factory", "repo-git");
        environment.setProperty("repository.design.comment-template.comment-validation-pattern", "EPBDS-\\d+.*");
        environment.setProperty("repository.design.comment-template.invalid-comment-message", "Start the comment with a ticket");

        var comment = service.getConfig("design").comment();

        assertNull(comment.userMessagePattern());
        assertNull(comment.invalidUserMessageHint());
    }
}
