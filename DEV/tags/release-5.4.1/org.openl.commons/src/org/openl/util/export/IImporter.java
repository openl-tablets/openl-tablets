package org.openl.util.export;

/**
 * Interface for reading persistent data of saved <code>IExportSection</code>s.
 */
public interface IImporter {
    /**
     * Reads rows of a section identified by <code>parentSectionId</code>.
     *
     * @param parentSectionId id of section to read rows for, or
     *            <code>null</code> for top level section.
     * @return array of <code>String[]</code>, where each element represent a
     *         row of data.
     */
    String[][] readRows(Object parentSectionId);

    /**
     * Reads all subsections of a section identified by
     * <code>parentSectionId</code>.
     *
     * @param parentSectionId id of section to read subsections of, or
     *            <code>null</code> for top level section
     * @return array of subsections of given section
     */
    IImportedSection[] readSections(Object parentSectionId);
}
