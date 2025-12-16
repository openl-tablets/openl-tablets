package org.openl.studio.projects.model.merge;

public record MergeResult(
        MergeResultStatus status,
        MergeConflictInfo conflictInfo
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private MergeConflictInfo conflictInfo;

        public Builder conflictInfo(MergeConflictInfo conflictInfo) {
            this.conflictInfo = conflictInfo;
            return this;
        }

        public MergeResult build() {
            var status = conflictInfo == null ? MergeResultStatus.SUCCESS : MergeResultStatus.CONFLICTS;
            return new MergeResult(status, conflictInfo);
        }
    }
}
