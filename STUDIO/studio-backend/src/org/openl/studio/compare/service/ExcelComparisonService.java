package org.openl.studio.compare.service;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import jakarta.validation.constraints.NotNull;

import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.studio.projects.service.ExecutionProgressListener;

/**
 * Compares two Excel files and reports how they differ.
 *
 * <p>Comparison parses both workbooks, so it is done away from the request thread and its progress
 * is told to a listener.
 */
public interface ExcelComparisonService {

    /**
     * Compares two Excel files.
     *
     * @param listener progress listener (must not be null)
     * @param first    the file the second one is compared against (must not be null)
     * @param second   the file compared against the first one (must not be null)
     * @return a future that completes with the tree of what the two files hold
     */
    CompletableFuture<DiffTreeNode> compare(@NotNull ExecutionProgressListener listener,
                                            @NotNull Path first,
                                            @NotNull Path second);
}
