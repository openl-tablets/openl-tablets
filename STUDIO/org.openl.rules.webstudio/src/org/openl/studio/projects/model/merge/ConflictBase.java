package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ConflictBase {

    @JsonProperty("base")
    BASE,

    @JsonProperty("ours")
    OURS,

    @JsonProperty("theirs")
    THEIRS

}
