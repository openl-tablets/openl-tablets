package org.openl.studio.projects.service.tests;

import java.util.List;
import java.util.function.Supplier;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.studio.projects.model.ExecutionValueLevels;
import org.openl.studio.projects.service.tables.TableModules;

/**
 * A test run that has ended.
 *
 * <p>It holds the results of every test table it ran, the way {@link RetainedTestUnit} keeps them, and reads their
 * values a level at a time.
 *
 * <p>It also keeps where its tables are written, as they were when it started: a test table of a project the tested
 * one depends on is opened in that project, whatever the session opens after the run.
 */
@RequiredArgsConstructor
public final class TestRun {

    /** The results of the test tables, in the order they ran. */
    @Getter
    @Accessors(fluent = true)
    private final List<TestUnitsResults> tables;

    @Getter
    @Accessors(fluent = true)
    private final List<String> notRun;

    /** The modules the tested tables are written in, each addressed by its project. */
    @Getter
    @Accessors(fluent = true)
    private final TableModules modules;

    private @Nullable ExecutionValueLevels levels;

    public TestRun(List<TestUnitsResults> tables, TableModules modules) {
        this(tables, List.of(), modules);
    }

    /**
     * Reads the values of the run a level at a time.
     *
     * <p>The reader is made on the first read and serves the run from then on. It reads with the object mapper of
     * the project, and making an object mapper defines classes in the project: made for every read, they would pile
     * up until the project is compiled again.
     *
     * @param reader makes the reader, on the first read
     */
    public synchronized ExecutionValueLevels levels(Supplier<ExecutionValueLevels> reader) {
        if (levels == null) {
            levels = reader.get();
        }
        return levels;
    }
}
