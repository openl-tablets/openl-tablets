package org.openl.studio.projects.service.tables;

import java.util.List;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.projects.model.tables.TableProperty;

/**
 * Reads and writes a table's own properties.
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

    /**
     * Writes the given properties onto the table, leaving the ones it is not told about as they are.
     *
     * <p>Only the properties section of the table is rewritten — the rows the values sit on are added, changed or
     * taken away — so a table of any size is edited without its body crossing the wire. A property given no value
     * is removed from the table; where it was inherited from a module or a category, that value applies again.
     *
     * <p>Where the installation records who last edited a table and when, that is recorded here too, as it is for
     * every other edit.
     *
     * @param table      the table to write to
     * @param properties the properties to write, each with the text its value is written as
     * @return the table's identifier after the write, which changes when the table had to be moved to grow
     */
    String write(IOpenLTable table, List<TableProperty> properties);
}
