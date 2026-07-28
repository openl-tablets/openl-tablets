package org.openl.rules.enumeration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum UsRegionsEnum {

    MW("Midwest"),
    NE("Northeast"),
    SE("Southeast"),
    SW("Southwest"),
    W("West");

    private final String displayName;

    @Override
    public String toString() {
        return displayName;
    }

    public static UsRegionsEnum fromString(String displayName) {
        for (UsRegionsEnum v : UsRegionsEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException("No constant with displayName '%s' is found.".formatted(displayName));
    }
}
