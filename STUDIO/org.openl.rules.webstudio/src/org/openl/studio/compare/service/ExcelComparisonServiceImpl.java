package org.openl.studio.compare.service;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import jakarta.validation.constraints.NotNull;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.xls2.XlsDiff2;
import org.openl.studio.projects.service.ExecutionLifecycle;
import org.openl.studio.projects.service.ExecutionProgressListener;

/**
 * Asynchronous implementation of {@link ExcelComparisonService}.
 *
 * <p>Comparison runs on the {@code comparisonExecutor} thread pool, so the request that starts it
 * returns at once and the screen follows the progress the listener reports.
 */
@Validated
@Service
public class ExcelComparisonServiceImpl implements ExcelComparisonService {

    @Override
    @Async("comparisonExecutor")
    public CompletableFuture<DiffTreeNode> compare(@NotNull ExecutionProgressListener listener,
                                                   @NotNull Path first,
                                                   @NotNull Path second) {
        return ExecutionLifecycle.execute(listener,
                () -> new XlsDiff2().diffFiles(first.toFile(), second.toFile()));
    }
}
