package org.openl.meta;

import org.apache.commons.lang.NotImplementedException;

/**
 * @author Andrei Astrouski
 */
public class Version implements Comparable<Version> {

    public static final String SEPARATOR = ".";

    private int major;
    private int minor;
    private int variant;

    public Version() {
    }

    public Version(int major, int minor, int variant) {
        this.major = major;
        this.minor = minor;
        this.variant = variant;
    }

    public Version(Version version) {
        this.major = version.getMajor();
        this.minor = version.getMinor();
        this.variant = version.getVariant();
    }

    public Version(String version) {
        setFullVersion(version);
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public int getVariant() {
        return variant;
    }

    public void setVariant(int variant) {
        this.variant = variant;
    }

    public String getFullVersion() {
        return major + SEPARATOR + minor + SEPARATOR + variant;
    }

    public void setFullVersion(String version) {
        if (version != null && version.replaceAll(" ", "").matches(
                "^[0-9]+\\" + SEPARATOR + "[0-9]+\\" + SEPARATOR + "[0-9]+$")) {
            String[] versionParts = version.split("\\" + SEPARATOR);
            major = Integer.valueOf(versionParts[0]);
            minor = Integer.valueOf(versionParts[1]);
            variant = Integer.valueOf(versionParts[2]);
        } else {
            throw new IllegalArgumentException("Incorrect format of version.");
        }
    }

    public int compareTo(Version to) {
        throw new NotImplementedException();
    }

    @Override
    public String toString() {
        return getFullVersion();
    }

    @Override
    public int hashCode() {
        return getFullVersion().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Version)) {
            return false;
        } else {
            return this.getFullVersion().equals(
                    ((Version) obj).getFullVersion());
        }
    }

}
