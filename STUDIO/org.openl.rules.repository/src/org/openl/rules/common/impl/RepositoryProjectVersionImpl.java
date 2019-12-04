package org.openl.rules.common.impl;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.VersionInfo;

public class RepositoryProjectVersionImpl implements ProjectVersion {
    private static final long serialVersionUID = -5156747482692477220L;

    private int major = MAX_MM_INT;
    private int minor = MAX_MM_INT;
    private String revision;
    private transient String versionName;
    private VersionInfo versionInfo;

    private String versionComment;
    private boolean deleted = false;

    @Override
    public String getVersionComment() {
        return versionComment;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public void setVersionComment(String versionComment) {
        this.versionComment = versionComment;
    }

    public RepositoryProjectVersionImpl(CommonVersion version, VersionInfo versionInfo) {
        major = version.getMajor();
        minor = version.getMinor();
        revision = version.getRevision();
        this.versionInfo = versionInfo;
    }

    public RepositoryProjectVersionImpl() {
        this("0", null, false);
    }

    public RepositoryProjectVersionImpl(String revision, VersionInfo versionInfo, boolean deleted) {
        this.revision = revision;
        this.versionInfo = versionInfo;
        this.deleted = deleted;
    }

    @Override
    public int compareTo(CommonVersion o) {
        if (revision.equals(o.getRevision())) {
            return 0;
        }

        /* Revision with #0 always should be at last place */
        if (revision.equals("0")) {
            return -1;
        }

        if (major != o.getMajor()) {
            return major < o.getMajor() ? -1 : 1;
        }

        if (minor != o.getMinor()) {
            return minor < o.getMinor() ? -1 : 1;
        }

        if (!revision.equals(o.getRevision())) {
            return revision.compareTo(o.getRevision());
        }

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ProjectVersion && compareTo((ProjectVersion) o) == 0;
    }

    @Override
    public int getMajor() {
        return major;
    }

    @Override
    public int getMinor() {
        return minor;
    }

    @Override
    public String getRevision() {
        return revision;
    }

    @Override
    public VersionInfo getVersionInfo() {
        return versionInfo;
    }

    @Override
    public String getVersionName() {
        if (versionName == null) {
            if (major != MAX_MM_INT && minor != MAX_MM_INT && major != -1 && minor != -1) {
                versionName = major + "." + minor + "." + revision;
            } else {
                versionName = revision;
            }
        }

        return versionName;
    }

    @Override
    public int hashCode() {
        int result;
        result = major;
        result = 31 * result + minor;
        result = 31 * result + revision.hashCode();
        return result;
    }

}
