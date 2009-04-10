package org.openl.util.export;

/**
 * Represents a unit of raw data for export. Can be a row in a database or in
 * excel table.
 */
public interface IExportRow {
    /**
     * Returns array of <code>String</code>s. The returned array must have
     * length equal to the value returned by {@link #size()} method.
     *
     * @return data array
     */
    String[] record();

    /**
     * Returns number of elements this row contains.
     *
     * @return length of the array returned by {@link #record()}
     */
    int size();
}
