package org.openl.rules.rest.model;

import java.time.ZonedDateTime;

import io.swagger.v3.oas.annotations.Parameter;

public class ProjectLockInfo {

    @Parameter(description = "Locked by user", required = true)
    public final String lockedBy;

    @Parameter(description = "Locked date and time", required = true)
    public final ZonedDateTime lockedAt;

    private ProjectLockInfo(Builder builder) {
        this.lockedBy = builder.lockedBy;
        this.lockedAt = builder.lockedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String lockedBy;
        private ZonedDateTime lockedAt;

        public Builder lockedBy(String lockedBy) {
            this.lockedBy = lockedBy;
            return this;
        }

        public Builder lockedAt(ZonedDateTime lockedAt) {
            this.lockedAt = lockedAt;
            return this;
        }

        public ProjectLockInfo build() {
            return new ProjectLockInfo(this);
        }
    }

}
