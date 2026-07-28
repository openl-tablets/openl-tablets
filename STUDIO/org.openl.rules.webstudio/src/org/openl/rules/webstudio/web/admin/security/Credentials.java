package org.openl.rules.webstudio.web.admin.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(implementation = String.class)
public class Credentials {

    @Getter
    @Parameter(description = "Login to check the connection to the LDAP server. It will not be saved anywhere.", example = "admin")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private final String username;

    @Getter
    @Parameter(description = "Password to check the connection to the LDAP server. It will not be saved anywhere.", example = "admin")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private final String password;

    @JsonCreator
    public static Credentials decode(String encoded) {
        var decoded = new String(java.util.Base64.getDecoder().decode(encoded));
        var parts = decoded.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid credentials value: " + encoded);
        }
        return new Credentials(parts[0], parts[1]);
    }
}
