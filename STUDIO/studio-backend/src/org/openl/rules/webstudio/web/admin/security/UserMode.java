package org.openl.rules.webstudio.web.admin.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum UserMode {

    SINGLE("single"),
    MULTI("multi"),
    AD("ad"),
    SAML("saml"),
    OAUTH2("oauth2");

    @Getter(onMethod_ = {@JsonValue})
    private final String value;

    @JsonCreator
    public static UserMode fromValue(String value) {
        return UserMode.valueOf(value.toUpperCase());
    }

}
