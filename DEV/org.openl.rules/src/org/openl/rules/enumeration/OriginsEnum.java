package org.openl.rules.enumeration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

// The constant names are the values of the origin property that tables and rules spell out.
@SuppressWarnings("java:S115")
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
