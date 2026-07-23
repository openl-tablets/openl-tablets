package org.openl.rules.webstudio.web.diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Repository;

class RepositoryDiffControllerTest {

    private static RulesProject projectOf(String repositoryId) {
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn(repositoryId);
        var project = mock(RulesProject.class);
        when(project.getDesignRepository()).thenReturn(repository);
        return project;
    }

    @Test
    void takesTheProjectOfTheRepositoryTheComparisonWasOpenedFrom() {
        var design = projectOf("design");
        var second = projectOf("design2");

        assertSame(second, RepositoryDiffController.pickProject(List.of(design, second), "design2").orElseThrow());
    }

    @Test
    void takesTheFirstProjectWhenNoRepositoryIsGiven() {
        var design = projectOf("design");
        var second = projectOf("design2");

        assertSame(design, RepositoryDiffController.pickProject(List.of(design, second), " ").orElseThrow());
    }

    @Test
    void findsNothingWhenNoProjectOfThatNameLivesInTheRepository() {
        assertTrue(RepositoryDiffController.pickProject(List.of(projectOf("design")), "design2").isEmpty());
        assertEquals(0, RepositoryDiffController.pickProject(List.of(), "design").stream().count());
    }
}
