package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.Module;

/** What a write to the open module asks for, and when the reader is the one who asks. */
class WebStudioWrittenModuleTest {

    private static WebStudio studio(boolean autoCompile) {
        var studio = mock(WebStudio.class, CALLS_REAL_METHODS);
        doReturn(new Module()).when(studio).getCurrentModule();
        doReturn(autoCompile).when(studio).isAutoCompile();
        return studio;
    }

    @Test
    void buildsTheModuleAgainAtOnceWhileCompilationIsAutomatic() {
        var studio = studio(true);

        studio.recompileCurrentModule();

        assertTrue(studio.isAwaitingRecompile(), "the written module is read again on the next request");
        assertFalse(studio.isManualCompileNeeded(), "and nothing is left for the reader to ask for");
    }

    @Test
    void leavesTheModuleForTheReaderToCompileWhileCompilationIsManual() {
        var studio = studio(false);

        studio.recompileCurrentModule();

        // Compiling after every edit is the wait the setting exists to avoid: the module stands as it was
        // compiled, and Verify is what builds it.
        assertFalse(studio.isAwaitingRecompile(), "the module is not built behind the reader's back");
        assertTrue(studio.isManualCompileNeeded(), "the screen is told there is something to verify");
    }

    @Test
    void buildsTheModuleAgainAfterARefusedWriteWhateverTheSettingSays() {
        var studio = studio(false);

        studio.rebuildCurrentModule();

        // A refused write leaves a workbook no author wrote; it cannot be left standing until Verify.
        assertTrue(studio.isAwaitingRecompile());
    }

    @Test
    void asksForNothingUntilSomethingIsWritten() {
        assertFalse(studio(false).isManualCompileNeeded());
        assertFalse(studio(true).isAwaitingRecompile());
    }
}
