package org.openl.rules.common.impl;

import java.util.StringTokenizer;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.VersionInfo;


public class RepositoryProjectVersionImpl implements ProjectVersion {
    private static final long serialVersionUID = -5156747482692477220L;
    public static final String DELIMETER = ".";

    private int major;
    private int minor;
    private int revision;
    private transient String versionName;
    private VersionInfo versionInfo;
    
    public RepositoryProjectVersionImpl(String version){
        StringTokenizer tokenizer = new StringTokenizer(version, DELIMETER);
        major = Integer.valueOf(tokenizer.nextToken());
        minor = Integer.valueOf(tokenizer.nextToken());
        revision = Integer.valueOf(tokenizer.nextToken());
    }

    public RepositoryProjectVersionImpl(CommonVersion version, VersionInfo versionInfo) {
        major = version.getMajor();
        minor = version.getMinor();
        revision = version.getRevision();
        this.versionInfo = versionInfo;
    }

    public RepositoryProjectVersionImpl(int major, int minor, int revision, VersionInfo versionInfo) {
        this.major = major;
        this.minor = minor;
        this.revision = revision;
        this.versionInfo = versionInfo;
    }

    public int compareTo(CommonVersion o) {
        if (major != o.getMajor()) {
            return major < o.getMajor() ? -1 : 1;
        }
        if (minor != o.getMinor()) {
            return minor < o.getMinor() ? -1 : 1;
        }
        if (revision != o.getRevision()) {
            return revision < o.getRevision() ? -1 : 1;
        }

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ProjectVersion && compareTo((ProjectVersion) o) == 0;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getRevision() {
        return revision;
    }

    public VersionInfo getVersionInfo() {
        return versionInfo;
    }

    public String getVersionName() {
        if (versionName == null) {
            versionName = new StringBuilder().append(major).append(".").append(minor).append(".").append(revision)
                    .toString();
        }

        return versionName;
    }

    @Override
    public int hashCode() {
        int result;
        result = major;
        result = 31 * result + minor;
        result = 31 * result + revision;
        return result;
    }
}
