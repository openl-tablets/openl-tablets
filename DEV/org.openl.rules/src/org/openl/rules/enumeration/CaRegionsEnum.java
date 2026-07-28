package org.openl.rules.enumeration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum CaRegionsEnum {

    QC("Québec"),
    HQ("Hors Québec");

    private final String displayName;

    @Override
    public String toString() {
        return displayName;
    }

    public static CaRegionsEnum fromString(String displayName) {
        for (CaRegionsEnum v : CaRegionsEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException("No constant with displayName '%s' is found.".formatted(displayName));
    }
}
