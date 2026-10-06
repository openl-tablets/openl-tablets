package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import org.openl.rules.project.model.Module;

/** What a write to the open module asks for, and when the reader is the one who asks. */
class WebStudioWrittenModuleTest {

    private static WebStudio studio(boolean autoCompile) {
        var studio = mock(WebStudio.class, CALLS_REAL_METHODS);
        // The mock skips the constructor, so the holders the real methods write to are supplied here.
        ReflectionTestUtils.setField(studio, "rewrittenModule", new AtomicReference<>());
        ReflectionTestUtils.setField(studio, "moduleToVerify", new AtomicReference<>());
        ReflectionTestUtils.setField(studio, "announcedWrite", new AtomicReference<>());
        var model = mock(ProjectModel.class);
        when(model.getCurrentCompilation()).thenReturn(RegisteredCompilation.completed());
        ReflectionTestUtils.setField(studio, "model", model);
        doReturn(new Module()).when(studio).getCurrentModule();
        doReturn(autoCompile).when(studio).isAutoCompile();
        return studio;
    }

    @Test
    void buildsTheModuleAgainAtOnceWhileCompilationIsAutomatic() {
        var studio = studio(true);

        studio.recompileCurrentModule();

        assertTrue(studio.isAwaitingRecompile(), "the written module is read again on the next request");
        // Building it again tells the screens how the project stands.
        verify(studio.getModel(), never()).publishStatusChanged();
    }

    @Test
    void leavesTheModuleForTheReaderToCompileWhileCompilationIsManual() {
        var studio = studio(false);

        studio.recompileCurrentModule();

        // Compiling after every edit is the wait the setting exists to avoid: the module stands as it was
        // compiled, and Verify is what builds it.
        assertFalse(studio.isAwaitingRecompile(), "the module is not built behind the reader's back");
        verify(studio.getModel()).publishStatusChanged();
    }

    @Test
    void tellsTheScreensOnceForEachCompilation() {
        var studio = studio(false);

        studio.recompileCurrentModule();
        studio.recompileCurrentModule();

        // The second write leaves the project waiting as the first one did.
        verify(studio.getModel()).publishStatusChanged();
        // Verify compiles the project again, and the next write leaves it waiting anew.
        when(studio.getModel().getCurrentCompilation()).thenReturn(RegisteredCompilation.completed());
        studio.recompileCurrentModule();
        verify(studio.getModel(), times(2)).publishStatusChanged();
    }

    @Test
    void buildsTheModuleAgainAfterARefusedWriteWhateverTheSettingSays() {
        var studio = studio(false);

        studio.rebuildCurrentModule();

        // A refused write leaves a workbook no author wrote; it cannot be left standing until Verify.
        assertTrue(studio.isAwaitingRecompile());
    }
}
