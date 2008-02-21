package org.openl.rules.repository;


public class CommonVersionImpl implements CommonVersion {
    private int major;
    private int minor;
    private int revision;
    
    private transient String versionName;

    public CommonVersionImpl(String s) {
        String[] version = s.split("\\.");
        
        if (version.length > 0) {
            major = Integer.parseInt(version[0], 10);
        }
        if (version.length > 1) {
            minor = Integer.parseInt(version[1], 10);
        }
        if (version.length > 2) {
            revision = Integer.parseInt(version[2], 10);
        }
    }
    
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

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommonVersion)) return false;

        return compareTo((CommonVersion) o) == 0;
    }

    public int hashCode() {
        return (major << 22) ^ (minor << 11) ^ revision;
    }
}
