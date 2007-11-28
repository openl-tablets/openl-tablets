package org.openl.rules.repository.jcr;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.openl.rules.repository.CommonUser;
import org.openl.rules.repository.CommonUserImpl;
import org.openl.rules.repository.CommonVersion;
import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.repository.RVersion;

/**
 * Implements JCR Version.
 * 
 * @author Aleh Bykhavets
 *
 */
public class JcrVersion implements RVersion {

    private Date lastModified;
    private String modifiedBy;

    private CommonVersionImpl version;

    protected static void create(Node node) throws RepositoryException {
        node.setProperty(JcrNT.PROP_VERSION, 0);
        node.setProperty(JcrNT.PROP_REVISION, 0);
    }
    
    public JcrVersion(Node node) {
        initVersion(node);
    }
    
    public JcrVersion(Version version) throws RepositoryException {
        // storing node's properties into variables to reduce 'throws' for getters
        Node frozen = version.getNode(JcrNT.FROZEN_NODE);
        
        initVersion(frozen);

        lastModified = version.getProperty("jcr:created").getDate().getTime();
//      if (frozen.hasProperty(JcrNT.PROP_MODIFIED_TIME)) {
//      lastModified = frozen.getProperty(JcrNT.PROP_MODIFIED_TIME).getDate().getTime();
//      }
        if (frozen.hasProperty(JcrNT.PROP_MODIFIED_BY)) { 
            modifiedBy = frozen.getProperty(JcrNT.PROP_MODIFIED_BY).getString();
        }
    }
    
    public JcrVersion(RVersion version) {
        this.version = new CommonVersionImpl(version);
    }

    public Date getCreated() {
        return lastModified;
    }

    public CommonUser getCreatedBy() {
        if (modifiedBy == null) {
            return new CommonUserImpl("system");
        } else {
            return new CommonUserImpl(modifiedBy);
        }
    }

    public String getVersionName() {
        return version.getVersionName();
    }

    public int getMajor() {
        return version.getMajor();
    }

    public int getMinor() {
        return version.getMinor();
    }

    public int getRevision() {
        return version.getRevision();
    }
    
    // --- protected
    
    protected void initVersion(Node node) {
        int major = 0;
        int minor = 0;
        long revision = 0;
        
        try {
            long l = node.getProperty(JcrNT.PROP_VERSION).getLong();
            int i = (int)l;
            major = i >> 16;
            minor = i & (0xFFFF);
        } catch (RepositoryException e) {
            // TODO: add logging
        }

        try {
            revision = node.getProperty(JcrNT.PROP_REVISION).getLong();
        } catch (RepositoryException e) {
            // TODO: add logging
        }
        
        version = new CommonVersionImpl(major, minor, (int)revision);
    }
    
    protected void nextRevision() {
        // only project can call this method
        int newRevision = version.getRevision();
        newRevision++;

        version = new CommonVersionImpl(version.getMajor(), version.getMinor(), newRevision);
    }
    
    protected void updateVersion(Node node) throws RepositoryException {
        long l = (version.getMajor() << 16) | (version.getMinor() & 0xFFFF);
        node.setProperty(JcrNT.PROP_VERSION, l);
        node.setProperty(JcrNT.PROP_REVISION, version.getRevision());
    }
    
    protected void updateRevision(Node node) throws RepositoryException {
        node.setProperty(JcrNT.PROP_REVISION, version.getRevision());
    }
    
    protected void set(int major, int minor, int revision) {
        version = new CommonVersionImpl(major, minor, revision);
    }

    public int compareTo(CommonVersion o) {
        return version.compareTo(o);
    }
}
