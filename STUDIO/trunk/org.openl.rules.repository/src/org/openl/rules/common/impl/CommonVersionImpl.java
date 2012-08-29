package org.openl.rules.common.impl;

import org.openl.rules.common.CommonVersion;

public class CommonVersionImpl implements CommonVersion {
    private int revision;

    private transient String versionName;

    public CommonVersionImpl(CommonVersion version) {
        revision = version.getRevision();
    }

    public CommonVersionImpl(int revision) {
        this.revision = revision;
    }

    public CommonVersionImpl(String s) {
        String[] version = s.split("\\.");

        if (version.length > 2) {
            revision = Integer.parseInt(version[2], 10);
        } else if (version.length == 1) {
            revision = Integer.parseInt(version[0], 10);
        }
    }

    public int compareTo(CommonVersion o) {
        if (revision != o.getRevision()) {
            return revision < o.getRevision() ? -1 : 1;
        }

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CommonVersion)) {
            return false;
        }

        return compareTo((CommonVersion) o) == 0;
    }

    public int getRevision() {
        return revision;
    }

    public String getVersionName() {
        if (versionName == null) {
            versionName = Integer.toString(revision);
        }

        return versionName;
    }

    @Override
    public int hashCode() {
        return revision;
    }
}
