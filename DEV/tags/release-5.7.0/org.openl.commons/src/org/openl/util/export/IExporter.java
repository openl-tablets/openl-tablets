package org.openl.util.export;

/**
 * An abstraction for {@link org.openl.util.export.IExportable} objects
 * persistence.
 */
public interface IExporter {
    /**
     * Persists an exportable object.
     *
     * @param exportable an object to export
     * @throws ExportException if an error while persisting occurs.
     */
    void persist(IExportable<?> exportable) throws ExportException;
}
