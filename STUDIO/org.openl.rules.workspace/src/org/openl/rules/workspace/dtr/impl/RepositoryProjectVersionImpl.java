package org.openl.rules.workspace.dtr.impl;

import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.abstracts.VersionInfo;

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
            versionName = new StringBuilder().append(major)
                    .append(".")
                    .append(minor)
                    .append(".")
                    .append(revision).toString();
        }

        return versionName;
    }

    public VersionInfo getVersionInfo() {
        return versionInfo;
    }
}
