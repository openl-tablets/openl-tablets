package org.openl.studio.projects.model.merge;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum MergeOpMode {

    @JsonProperty("receive")
    RECEIVE,

    @JsonProperty("send")
    SEND

}
