package org.openl.rules.spring.openapi.app110;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Mode {

    @JsonProperty("read")
    READ,
    @JsonProperty("write")
    WRITE
}
