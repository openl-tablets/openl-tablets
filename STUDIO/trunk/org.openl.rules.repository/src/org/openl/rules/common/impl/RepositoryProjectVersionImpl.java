package org.openl.rules.common.impl;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.VersionInfo;
import org.openl.rules.repository.RVersion;


public class RepositoryProjectVersionImpl implements ProjectVersion {
    private static final long serialVersionUID = -5156747482692477220L;
    public static final String DELIMETER = ".";

    private int revision;
    private transient String versionName;
    private VersionInfo versionInfo;
    private String versionComment;
    private Map<String, Object> versionProperties;

    public RepositoryProjectVersionImpl(String version){
        StringTokenizer tokenizer = new StringTokenizer(version, DELIMETER);
        revision = Integer.valueOf(tokenizer.nextToken());
    }

    public RepositoryProjectVersionImpl(CommonVersion version, VersionInfo versionInfo) {
        revision = version.getRevision();
        this.versionInfo = versionInfo;
    }

    public RepositoryProjectVersionImpl(int revision, VersionInfo versionInfo) {
        this.revision = revision;
        this.versionInfo = versionInfo;
    }

    public RepositoryProjectVersionImpl(CommonVersion version, VersionInfo versionInfo, String versionComment) {
        revision = version.getRevision();
        this.versionInfo = versionInfo;
        this.versionComment = versionComment;
    }

    public RepositoryProjectVersionImpl(CommonVersion version, VersionInfo versionInfo, String versionComment,
            Map<String, Object> versionProperties) {
        revision = version.getRevision();
        this.versionInfo = versionInfo;
        this.versionComment = versionComment;
        this.versionProperties = versionProperties;
    }

    public int compareTo(CommonVersion o) {
        if (revision != o.getRevision()) {
            return revision < o.getRevision() ? -1 : 1;
        }

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ProjectVersion && compareTo((ProjectVersion) o) == 0;
    }

    public int getRevision() {
        return revision;
    }

    public VersionInfo getVersionInfo() {
        return versionInfo;
    }

    public String getVersionName() {
        if (versionName == null) {
                versionName = Integer.toString(revision);
        }

        return versionName;
    }

    @Override
    public int hashCode() {
        int result;
        
        result = 31 * revision;
        return result;
    }
    
    public String getVersionComment() {
        if (versionComment != null) {
            return versionComment;
        } else {
            return "";
        }
    }
    
    public Map<String, Object> getVersionProperties() {
        return versionProperties;
    }

    public void setVersionProperties(Map<String, Object> versionProperties) {
        this.versionProperties = versionProperties;
    }
}
