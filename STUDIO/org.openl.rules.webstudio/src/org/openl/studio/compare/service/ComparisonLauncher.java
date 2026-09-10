package org.openl.studio.compare.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import org.openl.studio.compare.messaging.SocketComparisonProgressListenerFactory;
import org.openl.studio.compare.model.ComparisonStartedView;
import org.openl.studio.security.CurrentUserInfo;

/**
 * Starts comparisons, wherever the two files come from.
 *
 * <p>A comparison is named as it starts and runs on its own, so the caller answers with the name and
 * the result is read by it afterwards. The files a comparison reads belong to it: they are put down
 * when it is released, and taken back at once when it could not be started at all.
 */
@Component
@RequiredArgsConstructor
public class ComparisonLauncher {

    private final ExcelComparisonService comparisonService;
    private final ComparisonRegistry registry;
    private final ComparisonFileStore fileStore;
    private final SocketComparisonProgressListenerFactory listenerFactory;
    private final CurrentUserInfo currentUserInfo;

    /**
     * Starts comparing two uploaded files.
     *
     * @param first  the first uploaded file
     * @param second the second uploaded file
     * @return the name the comparison is read and watched by
     * @throws IOException when an upload cannot be written
     */
    public ComparisonStartedView start(MultipartFile first, MultipartFile second) throws IOException {
        return start(fileStore.store(first, second));
    }

    /**
     * Starts comparing two files that are already on disk.
     *
     * <p>The comparison reads copies of them, so whatever happens to the files afterwards - a version
     * restored, a project closed - leaves the comparison on screen alone.
     *
     * @param files the two files to compare
     * @return the name the comparison is read and watched by
     * @throws IOException when a file cannot be copied
     */
    public ComparisonStartedView startCopyOf(List<Path> files) throws IOException {
        return start(fileStore.copy(files));
    }

    /** Runs the comparison of the files the store has taken over. */
    private ComparisonStartedView start(List<Path> files) {
        var comparisonId = UUID.randomUUID().toString();
        try {
            var listener = listenerFactory.create(currentUserInfo.getUserName(), comparisonId);
            registry.register(comparisonId, files,
                    comparisonService.compare(listener, files.get(0), files.get(1)));
        } catch (RuntimeException e) {
            // Nothing owns the files until the comparison is registered. A comparison that could not
            // be started - the executor is full, the Studio is stopping - takes them with it.
            fileStore.delete(files);
            throw e;
        }
        return new ComparisonStartedView(comparisonId);
    }
}
