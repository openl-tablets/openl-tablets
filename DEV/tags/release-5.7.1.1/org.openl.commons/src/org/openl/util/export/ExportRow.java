package org.openl.util.export;

/**
 * Default implementation of {@link org.openl.util.export.IExportRow} interface.
 */
public class ExportRow implements IExportRow {
    private String[] rows;

    public ExportRow(String... rows) {
        this.rows = rows;
    }

    /**
     * Returns array of <code>String</code>s. The returned array must have
     * length equal to the value returned by {@link #size()} method.
     *
     * @return data array
     */
    public String[] record() {
        return rows;
    }

    /**
     * Returns number of elements this row contains.
     *
     * @return length of the array returned by {@link #record()}
     */
    public int size() {
        return rows.length;
    }
}
