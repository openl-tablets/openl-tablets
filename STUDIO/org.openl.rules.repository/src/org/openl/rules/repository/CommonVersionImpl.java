package org.openl.rules.repository;


public class CommonVersionImpl implements CommonVersion {
    private int major;
    private int minor;
    private int revision;
    
    private transient String versionName;

    public CommonVersionImpl(int major, int minor, int revision) {
        this.major = major;
        this.minor = minor;
        this.revision = revision;
    }
    
    public CommonVersionImpl(CommonVersion version) {
        major = version.getMajor();
        minor = version.getMinor();
        revision = version.getRevision();
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
            versionName = new StringBuilder()
            .append(major)
            .append(".")
            .append(minor)
            .append(".")
            .append(revision).toString();
        }

        return versionName;
    }
    
    public int compareTo(CommonVersion o) {
        int i;
        
        i = major - o.getMajor();
        if (i != 0) return i;
        
        i = minor - o.getMinor();
        if (i != 0) return i;
        
        return (revision - o.getRevision());
    }
}
