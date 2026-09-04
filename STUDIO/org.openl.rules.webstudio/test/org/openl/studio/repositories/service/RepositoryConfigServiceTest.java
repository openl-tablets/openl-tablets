package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.workspace.dtr.DesignTimeRepository;

class RepositoryConfigServiceTest {

    private final MockEnvironment environment = new MockEnvironment();
    private final DesignTimeRepository designTimeRepository = mock(DesignTimeRepository.class);
    private final RepositoryConfigService service = new RepositoryConfigService(environment, designTimeRepository);

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
    void gitRepositoryExposesItsConfiguredBranch() {
        environment.setProperty("repository.design.factory", "repo-git");
        var repository = mock(BranchRepository.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setBranches(true).build());
        when(repository.getBranch()).thenReturn("release/2026");
        when(designTimeRepository.getRepository("design")).thenReturn(repository);

        assertEquals("release/2026", service.getConfig("design").branch());
    }

    @Test
    void gitRepositoryExposesItsProtectedBranchPatterns() {
        environment.setProperty("repository.design.factory", "repo-git");
        environment.setProperty("repository.design.protected-branches", "master, release/* ,");

        // The blank entry left by a trailing comma is dropped, the rest are trimmed.
        assertEquals(List.of("master", "release/*"), service.getConfig("design").protectedBranches());
    }

    @Test
    void protectedBranchesAreEmptyWhenNoneAreConfigured() {
        environment.setProperty("repository.design.factory", "repo-git");

        assertEquals(List.of(), service.getConfig("design").protectedBranches());
    }

    @Test
    void protectedBranchesAreOmittedFromJsonWhenEmpty() throws Exception {
        environment.setProperty("repository.design.factory", "repo-git");

        var json = new ObjectMapper().writeValueAsString(service.getConfig("design"));

        assertFalse(json.contains("protectedBranches"));
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
