package org.openl.rules.spring.openapi.app020.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

public class ApiResponse {
    @Getter
    @JsonProperty("code")
    @Setter
    private Integer code;

    @Getter
    @JsonProperty("message")
    @Setter
    private String message;

    @Getter
    @JsonProperty("type")
    @Setter
    private String type;
}
