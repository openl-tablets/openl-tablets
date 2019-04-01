package org.openl.rules.validation.properties.dimentional;

import org.openl.rules.table.IWritableGrid;

/**
 * Interface for decision table column builder with POI.
 *
 * @author DLiauchuk
 *
 */
public interface IDecisionTableColumnBuilder {

    /**
     * Build the condition to the sheet with the given number of rules and start writing from the given column and row
     * indexes.
     *
     * @param gridModel the place for writing conditions
     * @param numberOfRules number of rules that will be written
     * @param columnStartIndex the index of the column on the sheet to start writing
     * @param rowStartIndex the index of the row on the sheet to start writing
     *
     * @return the index of the column that is next to the written condition and is free for further writing.
     */
    int build(IWritableGrid gridModel, int numberOfRules, int columnStartIndex, int rowStartIndex);
}
