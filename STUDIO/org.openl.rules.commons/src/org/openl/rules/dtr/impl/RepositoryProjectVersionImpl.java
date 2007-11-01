package org.openl.rules.dtr.impl;

import org.openl.rules.commons.projects.ProjectVersion;
import org.openl.rules.commons.projects.VersionInfo;

public class RepositoryProjectVersionImpl implements ProjectVersion {
    private int major;
    private int minor;
    private int revision;
    private String versionName;
    private VersionInfo versionInfo;

    public RepositoryProjectVersionImpl(int major, int minor, int revision, VersionInfo versionInfo) {
        this.major = major;
        this.minor = minor;
        this.revision = revision;
        this.versionInfo = versionInfo;
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

    public String getVersionName() {
        if (versionName == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(major);
            sb.append(".");
            sb.append(minor);
            sb.append(".");
            sb.append(revision);
        }

        return versionName;
    }

    public VersionInfo getVersionInfo() {
        return versionInfo;
    }
}
