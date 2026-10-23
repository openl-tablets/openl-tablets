package org.openl.studio.audit.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "How the user of an audit event signed in, or tried to.")
public enum AuditAuthentication {

    @Schema(description = "The login form of OpenL Studio, against its users, LDAP or Active Directory.")
    @JsonProperty("form")
    FORM,

    @Schema(description = "Basic authentication: a user name and a password sent with each request.")
    @JsonProperty("basic")
    BASIC,

    @Schema(description = "A personal access token sent with each request.")
    @JsonProperty("token")
    TOKEN,

    @Schema(description = "An OAuth2 bearer token of the identity provider sent with each request.")
    @JsonProperty("bearer")
    BEARER,

    @Schema(description = "A sign-in through OpenID Connect.")
    @JsonProperty("oauth2")
    OAUTH2,

    @Schema(description = "A sign-in through SAML.")
    @JsonProperty("saml")
    SAML,

    @Schema(description = "A transfer link, which moves one project file for the user who asked for it.")
    @JsonProperty("link")
    LINK
}
