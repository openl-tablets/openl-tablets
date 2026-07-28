package org.openl.rules.spring.openapi.app020.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

public class Tag {
    @Getter
    @JsonProperty("id")
    @Setter
    private Long id;

    @Getter
    @JsonProperty("name")
    @Setter
    private String name;
}
