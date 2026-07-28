package org.openl.rules.enumeration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum OriginsEnum {

    Base("Base"),
    Deviation("Deviation");

    private final String displayName;

    @Override
    public String toString() {
        return displayName;
    }

    public static OriginsEnum fromString(String displayName) {
        for (OriginsEnum v : OriginsEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException("No constant with displayName '%s' is found.".formatted(displayName));
    }
}
