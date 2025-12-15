package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CheckMergeStatus {

    @JsonProperty("mergeable")
    MERGEABLE,

    @JsonProperty("up-to-date")
    UP2DATE,
}
