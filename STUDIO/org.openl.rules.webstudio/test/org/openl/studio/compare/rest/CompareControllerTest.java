package org.openl.studio.compare.rest;

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
import org.openl.studio.compare.service.ComparisonFileStore;
import org.openl.studio.compare.service.ComparisonMapper;
import org.openl.studio.compare.service.ComparisonRegistry;
import org.openl.studio.compare.service.ExcelComparisonService;
import org.openl.studio.security.CurrentUserInfo;

@ExtendWith(MockitoExtension.class)
class CompareControllerTest {

    private static final List<Path> FILES = List.of(Path.of("first.xlsx"), Path.of("second.xlsx"));

    @Mock
    private ExcelComparisonService comparisonService;

    @Mock
    private ComparisonRegistry registry;

    @Mock
    private ComparisonFileStore fileStore;

    @Mock
    private ComparisonMapper mapper;

    @Mock
    private SocketComparisonProgressListenerFactory listenerFactory;

    @Mock
    private CurrentUserInfo currentUserInfo;

    @Test
    void namesTheComparisonItStarted() throws Exception {
        var task = new CompletableFuture<DiffTreeNode>();
        when(fileStore.store(any(), any())).thenReturn(FILES);
        when(comparisonService.compare(any(), any(), any())).thenReturn(task);

        var started = controller().compareFiles(upload("first.xlsx"), upload("second.xlsx"));

        assertNotNull(started.id());
        verify(registry).register(started.id(), FILES, task);
    }

    @Test
    void takesTheFilesBackWhenTheComparisonCannotBeStarted() throws Exception {
        when(fileStore.store(any(), any())).thenReturn(FILES);
        when(comparisonService.compare(any(), any(), any()))
                .thenThrow(new TaskRejectedException("No room for another comparison"));

        var failure = assertThrows(TaskRejectedException.class,
                () -> controller().compareFiles(upload("first.xlsx"), upload("second.xlsx")));

        assertEquals("No room for another comparison", failure.getMessage());
        // Nothing owns the files until the comparison is registered, so they must not be left behind.
        verify(fileStore).delete(FILES);
        verify(registry, never()).register(any(), any(), any());
    }

    private CompareController controller() {
        return new CompareController(comparisonService, registry, fileStore, mapper, listenerFactory,
                currentUserInfo);
    }

    private static MockMultipartFile upload(String name) {
        return new MockMultipartFile("file", name, null, new byte[0]);
    }
}
