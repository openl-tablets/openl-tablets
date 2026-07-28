package org.openl.rules.webstudio.web.admin.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum UserMode {

    SINGLE("single"),
    MULTI("multi"),
    AD("ad"),
    SAML("saml"),
    OAUTH2("oauth2");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static UserMode fromValue(String value) {
        return UserMode.valueOf(value.toUpperCase());
    }

}
