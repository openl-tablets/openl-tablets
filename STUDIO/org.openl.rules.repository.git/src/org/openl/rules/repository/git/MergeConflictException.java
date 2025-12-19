package org.openl.rules.repository.git;

import java.io.IOException;

/**
 * Exception thrown when a merge conflict occurs during a Git merge operation.
 */
public class MergeConflictException extends IOException {

    private final MergeConflictDetails details;

    public MergeConflictException(MergeConflictDetails details) {
        this.details = details;
    }

    public MergeConflictDetails getDetails() {
        return details;
    }
}
