package org.openl.rules.spring.openapi.app020.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

public class User {
    @Getter
    @JsonProperty("email")
    @Setter
    private String email;

    @Getter
    @JsonProperty("firstName")
    @Setter
    private String firstName;

    @Getter
    @JsonProperty("id")
    @Setter
    private Long id;

    @Getter
    @JsonProperty("lastName")
    @Setter
    private String lastName;

    @Getter
    @JsonProperty("password")
    @Setter
    private String password;

    @Getter
    @JsonProperty("phone")
    @Setter
    private String phone;

    @Getter
    @JsonProperty("userStatus")
    @Schema(description = "User Status")
    @Setter
    private Integer userStatus;

    @Getter
    @JsonProperty("username")
    @Setter
    private String username;
}
