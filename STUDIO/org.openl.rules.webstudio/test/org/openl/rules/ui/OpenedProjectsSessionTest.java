package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.Repository;

/**
 * Unit tests for the session's opened-project state machine: the current-selection/pin invariant, per-project
 * model lookup by identity, and the targeted invalidate/recompile/evict operations that keep one project's
 * compilation from clobbering another's. Collaborators (the shared compilation context and the model store) are
 * injected so behavior can be observed on mock models.
 */
class OpenedProjectsSessionTest {

    private static final String REPO = "design";
    private static final String BRANCH = "main";

    private WorkspaceCompilationContext context;
    private OpenedProjectsSession session;

    @BeforeEach
    void setUp() {
        context = mock(WorkspaceCompilationContext.class);
        var registry = new ProjectModelRegistry(key -> mock(ProjectModel.class), 8, m -> true);
        session = new OpenedProjectsSession(context, registry);
    }

    private static ProjectDescriptor descriptor(String folder) {
        ProjectDescriptor descriptor = mock(ProjectDescriptor.class);
        when(descriptor.getProjectFolder()).thenReturn(Path.of("/ws", folder));
        return descriptor;
    }

    private static RulesProject project(String repo, String businessName, String branch) {
        RulesProject project = mock(RulesProject.class);
        Repository repository = mock(Repository.class);
        when(repository.getId()).thenReturn(repo);
        when(project.getRepository()).thenReturn(repository);
        when(project.getBusinessName()).thenReturn(businessName);
        when(project.getBranch()).thenReturn(branch);
        return project;
    }

    @Test
    void currentModelNeverNullAndStableWhenNothingSelected() {
        assertNotNull(session.currentModel());
        assertSame(session.currentModel(), session.currentModel());
    }

    @Test
    void modelReturnsSameInstancePerProjectKey() {
        var a = descriptor("A");
        assertSame(session.model(a, REPO, BRANCH), session.model(a, REPO, BRANCH));
        assertNotSame(session.model(a, REPO, BRANCH), session.model(descriptor("B"), REPO, BRANCH));
    }

    @Test
    void selectMakesCurrentModelTheSelectedProjectsModel() {
        var a = descriptor("A");
        var model = session.model(a, REPO, BRANCH);

        session.select(a, REPO, BRANCH);

        assertSame(model, session.currentModel());
    }

    @Test
    void selectNoneResetsCurrentToTheEmptyModel() {
        var empty = session.currentModel();
        session.select(descriptor("A"), REPO, BRANCH);

        session.selectNone();

        assertSame(empty, session.currentModel());
    }

    @Test
    void openModuleBindsRepositoryAndOpensModuleWhenChanged() throws Exception {
        var descriptor = descriptor("A");
        var project = project(REPO, "ProjA", BRANCH);
        var module = mock(Module.class);
        var model = session.model(descriptor, REPO, BRANCH);

        assertSame(model, session.openModule(project, descriptor, module));

        verify(model).bindRepositoryId(REPO);
        verify(model).setModuleInfo(module);
    }

    @Test
    void openModuleSkipsReopeningWhenAlreadyOpen() throws Exception {
        var descriptor = descriptor("A");
        var project = project(REPO, "ProjA", BRANCH);
        var module = mock(Module.class);
        var model = session.model(descriptor, REPO, BRANCH);
        when(model.getModuleInfo()).thenReturn(module);

        session.openModule(project, descriptor, module);

        verify(model).bindRepositoryId(REPO);
        verify(model, never()).setModuleInfo(module);
    }

    @Test
    void modelIfPresentMatchesByProjectIdentity() {
        var project = project(REPO, "ProjA", BRANCH);
        var model = openedModel("A", project);

        assertSame(model, session.modelIfPresent(project));
        assertNull(session.modelIfPresent(project(REPO, "Other", BRANCH)));
    }

    @Test
    void evictInvalidatesCompiledModulesAndDropsModel() {
        var project = project(REPO, "ProjA", BRANCH);
        var moduleDescriptor = descriptor("A");
        var model = openedModel("A", project);
        when(model.getModuleInfo().getProject()).thenReturn(moduleDescriptor);

        session.evict(project);

        verify(context).invalidate(moduleDescriptor);
        verify(model).destroy();
        assertNull(session.modelIfPresent(project));
    }

    @Test
    void recompileReloadsOpenModelAndIgnoresUnopenedProject() {
        var project = project(REPO, "ProjA", BRANCH);
        var model = openedModel("A", project);

        session.recompile(project);

        verify(model).reloadAsync();
        assertDoesNotThrow(() -> session.recompile(project(REPO, "Other", BRANCH)));
    }

    @Test
    void removeStaleModelsDropsClosedProjectsAndKeepsOpenOnes() {
        var open = openedModel("Open", openedProject("Open", true));
        var closed = openedModel("Closed", openedProject("Closed", false));

        session.removeStaleModels();

        verify(closed).destroy();
        verify(open, never()).destroy();
    }

    @Test
    void resetSourceModifiedDelegatesToEveryModel() {
        var a = session.model(descriptor("A"), REPO, BRANCH);
        var b = session.model(descriptor("B"), REPO, BRANCH);

        session.resetSourceModified();

        verify(a).resetSourceModified();
        verify(b).resetSourceModified();
    }

    @Test
    void clearDestroysAllModelsAndShutsDownSharedContext() {
        var a = descriptor("A");
        var model = session.model(a, REPO, BRANCH);
        session.select(a, REPO, BRANCH);

        session.clear();

        verify(model).destroy();
        verify(context).shutdown();
        assertNotNull(session.currentModel());
    }

    @Test
    void dropCurrentSelectionDestroysAndUnpinsCurrentModel() {
        var a = descriptor("A");
        var model = session.model(a, REPO, BRANCH);
        session.select(a, REPO, BRANCH);
        assertSame(model, session.currentModel());

        session.dropCurrentSelection();

        verify(model).destroy();
        assertNotSame(model, session.currentModel());
    }

    @Test
    void sameProjectComparesRepositoryBusinessNameAndBranch() {
        var base = project(REPO, "P", BRANCH);

        assertTrue(session.sameProject(base, project(REPO, "P", BRANCH)));
        assertFalse(session.sameProject(base, project("other", "P", BRANCH)));
        assertFalse(session.sameProject(base, project(REPO, "Q", BRANCH)));
        assertFalse(session.sameProject(base, project(REPO, "P", "dev")));
    }

    /**
     * Create and register a model for {@code folder} that reports the given project as its own identity (a
     * non-null opened module plus {@code getProject()}), so identity-based lookups match it.
     */
    private ProjectModel openedModel(String folder, RulesProject project) {
        var model = session.model(descriptor(folder), REPO, BRANCH);
        when(model.getModuleInfo()).thenReturn(mock(Module.class));
        when(model.getProject()).thenReturn(project);
        return model;
    }

    private static RulesProject openedProject(String businessName, boolean opened) {
        var project = project(REPO, businessName, BRANCH);
        when(project.isOpened()).thenReturn(opened);
        return project;
    }
}
