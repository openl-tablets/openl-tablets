package org.openl.rules.enumeration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum RecalculateEnum {

    ALWAYS("Always"),
    NEVER("Never"),
    ANALYZE("Analyze");

    private final String displayName;

    @Override
    public String toString() {
        return displayName;
    }

    public static RecalculateEnum fromString(String displayName) {
        for (RecalculateEnum v : RecalculateEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException("No constant with displayName '%s' is found.".formatted(displayName));
    }
}
