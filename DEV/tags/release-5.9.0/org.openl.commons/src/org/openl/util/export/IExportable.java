package org.openl.util.export;

/**
 * If a class implements <i>IExportable</i> interface it can be persisted by
 * OpenL infrastructure to some storage, say Excel file. The class needs to
 * provide <i>IExportSection</i> describing persistence state.
 */
public interface IExportable<T> {
    /**
     * Returns class main section.
     *
     * @return IExportSection
     */
    IExportSection<T> mainSection();

    T restore(IImporter importer);
}
