package org.openl.studio.projects.service.tables;

import java.util.List;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.TableProperty;

/**
 * Reads a table's own properties for the copy dialog to prefill.
 *
 * @author Vladyslav Pikus
 */
public interface TablePropertiesService {

    /**
     * The table's own properties, with each value as the string the copy API writes it as: a date in ISO-8601, any
     * other value in the display form the Table Details editor shows.
     *
     * @param table the table to read
     * @return the properties defined on the table
     */
    List<TableProperty> read(IOpenLTable table);
}
