package org.openl.rules.repository.api;

public enum ChangesetType {
    /**
     * All files of a project
     */
    FULL,

    /**
     * Only changed files of a project
     */
    DIFF
}
