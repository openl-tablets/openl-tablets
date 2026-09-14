package org.openl.studio.compare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.mock.web.MockMultipartFile;

import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.studio.compare.messaging.SocketComparisonProgressListenerFactory;
import org.openl.studio.security.CurrentUserInfo;

@ExtendWith(MockitoExtension.class)
class ComparisonLauncherTest {

    private static final List<Path> STORED = List.of(Path.of("first.xlsx"), Path.of("second.xlsx"));

    @Mock
    private ExcelComparisonService comparisonService;

    @Mock
    private ComparisonRegistry registry;

    @Mock
    private ComparisonFileStore fileStore;

    @Mock
    private SocketComparisonProgressListenerFactory listenerFactory;

    @Mock
    private CurrentUserInfo currentUserInfo;

    @Test
    void namesTheComparisonItStarted() throws Exception {
        var task = new CompletableFuture<DiffTreeNode>();
        when(fileStore.store(any(), any())).thenReturn(STORED);
        when(comparisonService.compare(any(), any(), any())).thenReturn(task);

        var started = launcher().start(upload("first.xlsx"), upload("second.xlsx"));

        assertNotNull(started.id());
        verify(registry).register(started.id(), STORED, task);
    }

    @Test
    void comparesCopiesOfTheFilesItIsGiven() throws Exception {
        var versions = List.of(Path.of("history", "1700000000000"), Path.of("history", "Revision Version"));
        var task = new CompletableFuture<DiffTreeNode>();
        when(fileStore.copy(versions)).thenReturn(STORED);
        when(comparisonService.compare(any(), any(), any())).thenReturn(task);

        var started = launcher().startCopyOf(versions);

        // The comparison reads the copies, so restoring a version leaves what is on screen alone.
        verify(registry).register(started.id(), STORED, task);
        verify(comparisonService).compare(any(), any(Path.class), any(Path.class));
    }

    @Test
    void takesTheFilesBackWhenTheComparisonCannotBeStarted() throws Exception {
        when(fileStore.store(any(), any())).thenReturn(STORED);
        when(comparisonService.compare(any(), any(), any()))
                .thenThrow(new TaskRejectedException("No room for another comparison"));

        var launcher = launcher();
        var first = upload("first.xlsx");
        var second = upload("second.xlsx");

        var rejected = assertThrows(TaskRejectedException.class, () -> launcher.start(first, second));

        assertEquals("No room for another comparison", rejected.getMessage());
        // Nothing owns the files until the comparison is registered, so they must not be left behind.
        verify(fileStore).delete(STORED);
        verify(registry, never()).register(any(), any(), any());
    }

    private ComparisonLauncher launcher() {
        return new ComparisonLauncher(comparisonService, registry, fileStore, listenerFactory, currentUserInfo);
    }

    private static MockMultipartFile upload(String name) {
        return new MockMultipartFile("file", name, null, new byte[0]);
    }
}
