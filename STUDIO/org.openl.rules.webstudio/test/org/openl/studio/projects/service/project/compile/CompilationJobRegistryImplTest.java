package org.openl.studio.projects.service.project.compile;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.RegisteredCompilation;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.ProjectIdentifierMapper;

/**
 * Verifies the registry keeps one compilation job per opened project/branch so a session can track several
 * projects compiling in parallel without them clobbering each other.
 */
class CompilationJobRegistryImplTest {

    private WebStudio webStudio;
    private CompilationJobRegistryImpl registry;

    @BeforeEach
    void setUp() {
        webStudio = mock(WebStudio.class);
        registry = new CompilationJobRegistryImpl(webStudio, mock(ProjectIdentifierMapper.class));
    }

    private static ProjectModel modelOn(String branch) {
        var model = mock(ProjectModel.class);
        var project = mock(RulesProject.class);
        when(project.getBranch()).thenReturn(branch);
        when(model.getProject()).thenReturn(project);
        // A fresh, not-yet-completed cycle: stable identity (so tracksCurrentCompilation() holds) and cancellable.
        when(model.getCurrentCompilation()).thenReturn(new RegisteredCompilation(new CompletableFuture<>()));
        return model;
    }

    private static ProjectIdModel id(String repo, String name) {
        return ProjectIdModel.builder().repository(repo).projectName(name).build();
    }

    @Test
    void differentProjectsGetIndependentJobs() {
        var idA = id("design", "A");
        var idB = id("design", "B");

        var jobA = registry.acquire(idA, modelOn("main"));
        var jobB = registry.acquire(idB, modelOn("main"));

        assertNotSame(jobA, jobB);
        assertSame(jobA, registry.find(idA, "main").orElseThrow());
        assertSame(jobB, registry.find(idB, "main").orElseThrow());
    }

    @Test
    void sameProjectReusesJob() {
        var idA = id("design", "A");
        var model = modelOn("main");

        assertSame(registry.acquire(idA, model), registry.acquire(idA, model));
    }

    @Test
    void clearSingleProjectLeavesOthersAndCancelsInFlight() {
        var idA = id("design", "A");
        var idB = id("design", "B");
        var jobA = registry.acquire(idA, modelOn("main"));
        var jobB = registry.acquire(idB, modelOn("main"));

        registry.clear(idA, "main");

        assertTrue(registry.find(idA, "main").isEmpty());
        assertSame(jobB, registry.find(idB, "main").orElseThrow());
        assertTrue(jobA.future().isCancelled());
    }

    @Test
    void workspaceResetClearsAll() {
        var idA = id("design", "A");
        var idB = id("design", "B");
        registry.acquire(idA, modelOn("main"));
        registry.acquire(idB, modelOn("main"));

        registry.onWorkspaceReset(new org.openl.rules.ui.WorkspaceResetEvent(webStudio));

        assertTrue(registry.find(idA, "main").isEmpty());
        assertTrue(registry.find(idB, "main").isEmpty());
    }

    @Test
    void sameProjectDifferentBranchesAreDistinct() {
        var idA = id("design", "A");

        var jobMain = registry.acquire(idA, modelOn("main"));
        var jobDev = registry.acquire(idA, modelOn("dev"));

        assertNotSame(jobMain, jobDev);
        assertSame(jobMain, registry.find(idA, "main").orElseThrow());
        assertSame(jobDev, registry.find(idA, "dev").orElseThrow());
    }
}
