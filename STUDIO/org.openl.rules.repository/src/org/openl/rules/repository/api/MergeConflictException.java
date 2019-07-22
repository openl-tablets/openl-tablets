package org.openl.rules.repository.api;

import java.io.IOException;
import java.util.Collection;

public class MergeConflictException extends IOException {
    private final Collection<String> conflictedFiles;
    private final String ourCommit;
    private final String theirCommit;
    private final String baseCommit;

    public MergeConflictException(Collection<String> conflictedFiles,
            String baseCommit,
            String ourCommit,
            String theirCommit) {
        this.conflictedFiles = conflictedFiles;
        this.baseCommit = baseCommit;
        this.ourCommit = ourCommit;
        this.theirCommit = theirCommit;
    }

    public Collection<String> getConflictedFiles() {
        return conflictedFiles;
    }

    public String getOurCommit() {
        return ourCommit;
    }

    public String getTheirCommit() {
        return theirCommit;
    }

    public String getBaseCommit() {
        return baseCommit;
    }

}
