package org.openl.studio.projects.service.resources;

/**
 * Defines how project resources should be returned.
 *
 */
public enum ResourceViewMode {

    /**
     * Returns a flat list of all resources.
     * Resources are sorted with folders first, then files, both alphabetically.
     */
    FLAT,

    /**
     * Returns a nested tree structure.
     * FolderResource instances contain their children in the 'children' field.
     * When recursive=false, folders have empty children arrays.
     * When recursive=true, the full tree structure is built.
     */
    NESTED
}
