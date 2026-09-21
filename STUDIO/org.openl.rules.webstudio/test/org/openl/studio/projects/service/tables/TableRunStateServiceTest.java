package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.tables.TableRunState;

/**
 * What a table is as something to run: the rule the Editor's own toolbar has always followed, which offered
 * Run, Trace and Benchmark only where there was something to run.
 *
 * <p>A table the compiler could not build runs nothing, and neither does a test of rules that failed — it has
 * nothing to test.
 */
class TableRunStateServiceTest {

    @TempDir
    Path tempDir;

    private final TableRunStateService service = new TableRunStateService();

    @Test
    void a_table_that_compiled_runs() throws IOException {
        var model = TableTestProjects.projectModel(sound("sound"));

        // Only the module the reader opened is built, so a run of it stays inside that module.
        assertEquals(TableRunState.CAN_RUN_MODULE, service.of(model, tableNamed(model, "Hello")));
    }

    @Test
    void a_test_runs_while_the_rules_it_exercises_compile() throws IOException {
        var model = TableTestProjects.projectModel(sound("tested"));

        assertEquals(TableRunState.CAN_RUN_MODULE, service.of(model, tableNamed(model, "HelloTest")));
    }

    @Test
    void a_table_the_compiler_could_not_build_runs_nothing() throws IOException {
        var model = TableTestProjects.projectModel(broken("broken"));

        assertEquals(TableRunState.CANNOT_RUN, service.of(model, tableNamed(model, "Hello")));
    }

    @Test
    void a_test_of_rules_that_failed_runs_nothing_either() throws IOException {
        var model = TableTestProjects.projectModel(broken("broken-tested"));

        // The test itself is written well enough; the rules it exercises are not.
        assertEquals(TableRunState.CANNOT_RUN, service.of(model, tableNamed(model, "HelloTest")));
    }

    /** A module whose rules compile, with a test written against them. */
    private Path sound(String name) throws IOException {
        return TableTestProjects.writeProject(tempDir.resolve(name), name, "Rules", rules("make == c1"));
    }

    /** The same module, with a condition naming something the table does not declare. */
    private Path broken(String name) throws IOException {
        return TableTestProjects.writeProject(tempDir.resolve(name), name, "Rules", rules("make == nosuchthing"));
    }

    /** One decision table under the given condition, and a test written against it. */
    private static String[][] rules(String condition) {
        return new String[][]{
                {"Rules String Hello(String make)", null, null},
                {"C1", "RET1", null},
                {condition, "value", null},
                {"String c1", "String value", null},
                {"Make", "Price", null},
                {"Toyota", "Cheap", null},
                {null, null, null},
                {"Test Hello HelloTest", null, null},
                {"make", "_res_", null},
                {"Make", "Expected", null},
                {"Toyota", "Cheap", null}
        };
    }

    /** The compiled table of the module carrying the given name. */
    private static IOpenLTable tableNamed(ProjectModel model, String name) {
        return model.getAllTableSyntaxNodes()
                .stream()
                .map(TableSyntaxNodeAdapter::new)
                .filter(table -> name.equals(table.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no table named " + name));
    }
}
