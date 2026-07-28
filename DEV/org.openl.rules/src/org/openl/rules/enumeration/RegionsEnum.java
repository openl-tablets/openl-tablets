package org.openl.rules.enumeration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum RegionsEnum {

    NCSA("Americas"),
    EU("European Union"),
    EMEA("Europe; Middle East; Africa"),
    APJ("Asia Pacific; Japan");

    private final String displayName;

    @Override
    public String toString() {
        return displayName;
    }

    public static RegionsEnum fromString(String displayName) {
        for (RegionsEnum v : RegionsEnum.values()) {
            if (displayName.equalsIgnoreCase(v.displayName)) {
                return v;
            }
        }

        throw new IllegalArgumentException("No constant with displayName '%s' is found.".formatted(displayName));
    }
}
