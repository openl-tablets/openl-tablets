package org.openl.studio.projects.service.project.compile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.Module;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.RegisteredCompilation;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.ProjectIdentifierMapper;

class CompilationJobRegistryImplTest {

    private static final ProjectIdModel PROJECT = ProjectIdModel.builder()
            .repository("design")
            .projectName("Rating")
            .build();

    private WebStudio webStudio;
    private CompilationJobRegistryImpl registry;

    @BeforeEach
    void setUp() {
        webStudio = mock(WebStudio.class);
        var projectIdentifierMapper = mock(ProjectIdentifierMapper.class);
        registry = new CompilationJobRegistryImpl(webStudio, projectIdentifierMapper);

        var project = mock(RulesProject.class);
        when(project.getBranch()).thenReturn("main");
        when(webStudio.getCurrentProject()).thenReturn(project);
        when(webStudio.getCurrentModule()).thenReturn(mock(Module.class));
        when(projectIdentifierMapper.map(project)).thenReturn(PROJECT);

        var compilation = mock(RegisteredCompilation.class);
        when(compilation.future()).thenReturn(CompletableFuture.completedFuture(null));
        var model = mock(ProjectModel.class);
        when(model.getCurrentCompilation()).thenReturn(compilation);
        when(webStudio.getModel()).thenReturn(model);
    }

    @Test
    void acquiresAJobForAModelThatHoldsNoProject() {
        var compilation = mock(RegisteredCompilation.class);
        when(compilation.future()).thenReturn(CompletableFuture.completedFuture(null));
        var model = mock(ProjectModel.class);
        when(model.getCurrentCompilation()).thenReturn(compilation);

        assertNotNull(registry.acquire(PROJECT, model));
    }

    @Test
    void reportsWhatTheSessionIsCompilingWhenNothingHasChangedUnderIt() {
        assertTrue(registry.find(PROJECT, "main").isPresent());
    }

    @Test
    void reportsNothingWhileAWriteWaitsToBeCompiled() {
        // What the session compiled was worked out from the workbook as it stood before the write. Reporting it
        // would tell the reader the table they have just broken still compiles.
        when(webStudio.isAwaitingRecompile()).thenReturn(true);

        assertTrue(registry.find(PROJECT, "main").isEmpty());
    }
}
