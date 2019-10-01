package org.openl.rules.project.xml;

public enum SupportedVersion {
    V5_11,
    V5_12,
    V5_13,
    V5_14,
    V5_15,
    V5_16,
    V5_17,
    V5_18,
    V5_19,
    V5_20,
    V5_21,
    V5_22,
    V5_23;
    

    public String getVersion() {
        return name().substring(1).replace("_", ".");
    }

    public boolean isLastVersion() {
        return this == getLastVersion();
    }

    public static SupportedVersion getByVersion(String version) {
        for (SupportedVersion supportedVersion : values()) {
            if (supportedVersion.getVersion().equals(version)) {
                return supportedVersion;
            }
        }

        throw new UnsupportedOperationException("Unsupported OpenL version " + version);
    }

    public static SupportedVersion getLastVersion() {
        SupportedVersion[] supportedVersions = values();
        return supportedVersions[supportedVersions.length - 1];
    }
}
