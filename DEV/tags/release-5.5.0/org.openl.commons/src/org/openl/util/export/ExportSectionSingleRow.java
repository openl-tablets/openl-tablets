package org.openl.util.export;

/**
 * A simple implementation of <code>IExportSection</code> which is just a
 * named section with a single row.
 *
 *
 * @author Aliaksandr Antonik.
 */
public class ExportSectionSingleRow implements IExportSection<ExportSectionSingleRow> {
    private IExportRow[] rows;
    private String name;

    public ExportSectionSingleRow(String name, String[] row) {
        this.name = name;
        rows = new IExportRow[] { new ExportRow(row) };
    }

    /**
     * Returns the java class this section represents.
     *
     * @return a class
     */
    @SuppressWarnings("unchecked")
    public Class getExportedClass() {
        return getClass();
    }

    /**
     * Returns section name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Return array of rows - section data. Can be <code>null</code>.
     *
     * @return section rows.
     */
    public IExportRow[] getRows() {
        return rows;
    }

    /**
     * Returns array of subsections of this section. Can be <code>null</code>.
     *
     * @return child sections
     */
    @SuppressWarnings("unchecked")
    public IExportSection[] getSubSections() {
        return null;
    }
}
