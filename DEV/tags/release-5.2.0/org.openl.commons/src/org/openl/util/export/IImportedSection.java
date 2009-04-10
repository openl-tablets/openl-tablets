package org.openl.util.export;

/**
 * Represents a section while importing a persistent object.
 *
 * @see org.openl.util.export.IImporter
 */
public interface IImportedSection {
    /**
     * Returns section id to be used for traversal object tree with
     * {@link org.openl.util.export.IImporter}.
     *
     * @return section id
     */
    Object getId();

    /**
     * Returns section name
     *
     * @return name
     */
    String getName();
}
