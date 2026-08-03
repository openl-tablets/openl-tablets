package org.openl.studio.projects.service.tables;

import java.util.List;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.TableProperty;

/**
 * Reads a table's own properties for the copy dialog to prefill, in the display form the Table Details editor shows.
 *
 * @author Vladyslav Pikus
 */
public interface TablePropertiesService {

    /**
     * The table's own properties, with each value in its display string form.
     *
     * @param table the table to read
     * @return the properties defined on the table
     */
    List<TableProperty> read(IOpenLTable table);
}
