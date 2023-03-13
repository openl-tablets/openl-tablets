package org.openl.rules.repository.git;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.openl.rules.xls.merge.diff.WorkbookDiffResult;

public class MergeConflictException extends IOException {
    private final Map<String, String> diffs;
    private final String yourCommit;
    private final String theirCommit;
    private final String baseCommit;
    private final Map<String, WorkbookDiffResult> toAutoResolve;

    public MergeConflictException(Map<String, String> diffs,
            String baseCommit,
            String yourCommit,
            String theirCommit,
            Map<String, WorkbookDiffResult> toAutoResolve) {
        this.baseCommit = baseCommit;
        this.yourCommit = yourCommit;
        this.theirCommit = theirCommit;
        this.diffs = diffs;
        this.toAutoResolve = Collections.unmodifiableMap(toAutoResolve);
    }

    public Collection<String> getConflictedFiles() {
        return diffs.keySet();
    }

    public String getYourCommit() {
        return yourCommit;
    }

    public String getTheirCommit() {
        return theirCommit;
    }

    public String getBaseCommit() {
        return baseCommit;
    }

    public Map<String, String> getDiffs() {
        return diffs;
    }

    public Map<String, WorkbookDiffResult> getToAutoResolve() {
        return toAutoResolve;
    }
}
