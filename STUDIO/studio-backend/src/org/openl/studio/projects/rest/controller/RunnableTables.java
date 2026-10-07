package org.openl.studio.projects.rest.controller;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.service.tables.GeneratedTables;

/**
 * Finds the table a run, a trace, a benchmark or a test run is started from.
 *
 * <p>A table the compiler built to choose between the versions of an overloaded rule is not one of them: it is
 * refused as {@link GeneratedTables} tells.
 */
final class RunnableTables {

    private RunnableTables() {
    }

    /**
     * The table with the given identifier, to be run.
     *
     * @param projectModel the compiled project
     * @param tableId      the identifier the Tables API addresses the table by
     * @return the table
     * @throws NotFoundException   when no table has the identifier
     * @throws BadRequestException when the compiler built the table
     */
    static IOpenLTable require(ProjectModel projectModel, String tableId) {
        var table = projectModel.getTableById(tableId);
        if (table == null) {
            throw new NotFoundException("table.message");
        }
        GeneratedTables.refuseAction(projectModel, table);
        return table;
    }
}
