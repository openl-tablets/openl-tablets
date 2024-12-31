package org.openl.rules.project.xml;

public enum SupportedVersion {
    V5_23,
    V5_24,
    V5_25,
    V5_26,
    V5_27;

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
