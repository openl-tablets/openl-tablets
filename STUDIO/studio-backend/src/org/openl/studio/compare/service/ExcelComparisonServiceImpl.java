package org.openl.studio.compare.service;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import jakarta.validation.constraints.NotNull;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import org.openl.rules.xls.merge.XlsConflictComparator;
import org.openl.rules.xls.merge.XlsWorkbookComparator;
import org.openl.rules.xls.merge.compare.ConflictComparison;
import org.openl.rules.xls.merge.compare.WorkbookComparison;
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
    public CompletableFuture<WorkbookComparison> compare(@NotNull ExecutionProgressListener listener,
                                                         @NotNull Path first,
                                                         @NotNull Path second) {
        return ExecutionLifecycle.execute(listener,
                () -> XlsWorkbookComparator.compare(first.toFile(), second.toFile()));
    }

    @Override
    @Async("comparisonExecutor")
    public CompletableFuture<ConflictComparison> compare(@NotNull ExecutionProgressListener listener,
                                                         @NotNull Path base,
                                                         @NotNull Path yours,
                                                         @NotNull Path theirs) {
        return ExecutionLifecycle.execute(listener,
                () -> XlsConflictComparator.compare(base.toFile(), yours.toFile(), theirs.toFile()));
    }
}
