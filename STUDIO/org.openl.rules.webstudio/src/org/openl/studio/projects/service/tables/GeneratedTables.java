package org.openl.studio.projects.service.tables;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.common.exception.BadRequestException;

/**
 * Keeps the table the compiler built to choose between the versions of an overloaded rule for reading only.
 *
 * <p>Such a table is built again from those versions at every compilation, so nothing written to it would last and
 * running it would run no rule of the project. Writing, copying, running, tracing and benchmarking it are refused,
 * saying why, as OpenL Studio never offered them for it.
 */
public final class GeneratedTables {

    private GeneratedTables() {
    }

    /**
     * Refuses an action on a table the compiler built.
     *
     * @param model the compiled project the table was resolved through
     * @param table the table the action is asked for
     * @throws BadRequestException when the compiler built the table
     */
    public static void refuseAction(ProjectModel model, IOpenLTable table) {
        if (model.isGeneratedTable(table)) {
            throw new BadRequestException("table.generated.message", new Object[]{table.getName()});
        }
    }
}
