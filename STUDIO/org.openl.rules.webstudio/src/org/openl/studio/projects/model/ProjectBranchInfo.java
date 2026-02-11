package org.openl.studio.projects.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProjectBranchInfo(
        @Schema(description = "Branch name")
        String name,

        @Schema(description = "Whether the branch is protected")
        @JsonProperty("protected")
        boolean protectedFlag
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private boolean protectedFlag;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder protectedFlag(boolean protectedFlag) {
            this.protectedFlag = protectedFlag;
            return this;
        }

        public ProjectBranchInfo build() {
            return new ProjectBranchInfo(name,
                    protectedFlag);
        }
    }
}
