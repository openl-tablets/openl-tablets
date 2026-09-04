package org.openl.rules.enumeration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum DTEmptyResultProcessingEnum {

    SKIP("Skip"),
    RETURN("Return");

    private final String displayName;

    @Override
    public String toString() {
        return displayName;
    }

    public static DTEmptyResultProcessingEnum fromString(String displayName) {
        for (DTEmptyResultProcessingEnum v : DTEmptyResultProcessingEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException("No constant with displayName '%s' is found.".formatted(displayName));
    }
}
