package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum MergeResultStatus {
    @JsonProperty("success")
    SUCCESS,
    @JsonProperty("conflicts")
    CONFLICTS
}
