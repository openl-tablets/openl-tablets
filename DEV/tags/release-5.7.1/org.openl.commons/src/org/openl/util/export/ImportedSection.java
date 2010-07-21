package org.openl.util.export;

/**
 * Simple implementation of <code>IImportedSection</code> interface.
 */
public class ImportedSection implements IImportedSection {
    private String name;
    private Object id;

    public ImportedSection(Object id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Returns section id to be used for traversal object tree with
     * {@link IImporter}.
     *
     * @return section id
     */
    public Object getId() {
        return id;
    }

    /**
     * Returns section name
     *
     * @return name
     */
    public String getName() {
        return name;
    }
}
