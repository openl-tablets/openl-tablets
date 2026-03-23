package org.openl.studio.projects.service.run;

import java.util.concurrent.CompletableFuture;
import jakarta.validation.constraints.NotNull;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.service.AbstractMethodExecutorService;
import org.openl.studio.projects.service.ExecutionProgressListener;

/**
 * Asynchronous implementation of {@link RunExecutorService}.
 * <p>
 * Executes run operations on the {@code testSuiteExecutor} thread pool to avoid
 * blocking HTTP request threads.
 * </p>
 */
@Validated
@Component
public class RunExecutorServiceImpl extends AbstractMethodExecutorService implements RunExecutorService {

    @Override
    @Async("testSuiteExecutor")
    public CompletableFuture<TestUnitsResults> runMethod(@NotNull ExecutionProgressListener listener,
                                                         @NotNull ProjectModel projectModel,
                                                         @NotNull IOpenLTable table,
                                                         Object[] params,
                                                         IRulesRuntimeContext runtimeContext,
                                                         boolean currentOpenedModule) {
        return executeWithLifecycle(listener, () -> {
            var method = resolveMethod(projectModel, table, currentOpenedModule, runtimeContext);
            var db = getDb(projectModel, currentOpenedModule);
            var testSuite = new TestSuite(new TestDescription(method, runtimeContext, params, db));
            return projectModel.runTest(testSuite, currentOpenedModule);
        });
    }
}
