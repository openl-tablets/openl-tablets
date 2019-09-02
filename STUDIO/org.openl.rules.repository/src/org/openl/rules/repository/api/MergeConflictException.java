package org.openl.rules.repository.api;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class MergeConflictException extends IOException {
    private final Map<String, String> diffs;
    private final String yourCommit;
    private final String theirCommit;
    private final String baseCommit;

    public MergeConflictException(Map<String, String> diffs, String baseCommit, String yourCommit, String theirCommit) {
        this.baseCommit = baseCommit;
        this.yourCommit = yourCommit;
        this.theirCommit = theirCommit;
        this.diffs = diffs;
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
}
