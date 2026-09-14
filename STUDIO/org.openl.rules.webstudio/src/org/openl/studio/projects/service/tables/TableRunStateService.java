package org.openl.studio.projects.service.tables;

import org.springframework.stereotype.Service;

import org.openl.message.Severity;
import org.openl.rules.rest.compile.OpenLTableLogic;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.tables.TableRunState;

/**
 * Says whether a table can be run as it stands, the way the Editor has always decided it.
 *
 * <p>A table the compiler could not build runs nothing: running it would only report the error again. A test or
 * a run table is judged by the rules it exercises as well — a test of a table that failed to compile has
 * nothing to test.
 *
 * <p>Where the answer is yes, it also says how far the run may reach. While only the opened module is built,
 * and where what is built beyond it has errors, a run stays inside the module the table is written in.
 *
 * @author Vladyslav Pikus
 */
@Service
public class TableRunStateService {

    /**
     * How the table stands as something to run.
     *
     * @param model the compiled module the table is read through
     * @param table the table being asked about
     * @return what a screen may offer for it
     */
    public TableRunState of(ProjectModel model, IOpenLTable table) {
        var compiledThrough = model.isProjectCompilationCompleted();
        var uri = table.getUri();
        if (!model.getOpenedModuleMessagesByTsn(uri, Severity.ERROR).isEmpty()) {
            return TableRunState.CANNOT_RUN;
        }
        var state = compiledThrough ? TableRunState.CAN_RUN : TableRunState.CAN_RUN_MODULE;
        if (compiledThrough && !model.getMessagesByTsn(uri, Severity.ERROR).isEmpty()) {
            // The table is sound in its own module and broken once the rest of the project is with it.
            state = TableRunState.CAN_RUN_MODULE;
        }
        return againstTheRulesItExercises(model, table, compiledThrough, state);
    }

    /**
     * The same answer, narrowed by the tables a test or a run table exercises.
     *
     * <p>Rules that failed to compile in the module the test is read through leave it nothing to run at all;
     * rules that fail only once the whole project is built leave it its own module to run in.
     */
    private static TableRunState againstTheRulesItExercises(ProjectModel model,
                                                            IOpenLTable table,
                                                            boolean compiledThrough,
                                                            TableRunState state) {
        var narrowed = state;
        for (var target : OpenLTableLogic.getTargetTables(table, model, !compiledThrough)) {
            if (model.getErrorsByUri(target.getUri()).isEmpty()) {
                continue;
            }
            if (!model.getOpenedModuleMessagesByTsn(target.getUri(), Severity.ERROR).isEmpty()) {
                return TableRunState.CANNOT_RUN;
            }
            narrowed = TableRunState.CAN_RUN_MODULE;
        }
        return narrowed;
    }
}
