package org.openl.studio.projects.service.tables;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.TableDetailsView;

/**
 * Reads what a table says about itself besides its cells.
 *
 * @author Vladyslav Pikus
 */
public interface TableDetailsService {

    /**
     * The name of the table and the properties that apply to it, grouped as the property dictionary groups them.
     *
     * @param table table to describe
     * @return the table's details, with an empty group list for a kind of table that carries no properties
     */
    TableDetailsView read(IOpenLTable table);

}
