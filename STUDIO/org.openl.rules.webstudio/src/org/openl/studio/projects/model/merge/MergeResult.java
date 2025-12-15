package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MergeResult(
        Status status,
        MergeConflictInfo conflictInfo
) {

    public static Builder builder() {
        return new Builder();
    }

    public enum Status {
        @JsonProperty("success")
        SUCCESS,
        @JsonProperty("conflicts")
        CONFLICTS
    }

    public static class Builder {
        private MergeConflictInfo conflictInfo;

        public Builder conflictInfo(MergeConflictInfo conflictInfo) {
            this.conflictInfo = conflictInfo;
            return this;
        }

        public MergeResult build() {
            var status = conflictInfo == null ? Status.SUCCESS : Status.CONFLICTS;
            return new MergeResult(status, conflictInfo);
        }
    }
}
