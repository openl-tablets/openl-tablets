package org.openl.rules.repository.jcr;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.openl.rules.repository.RUser;
import org.openl.rules.repository.RVersion;

/**
 * Implements JCR Version.
 * 
 * @author Aleh Bykhavets
 *
 */
public class JcrVersion implements RVersion {

    // temporary variables, just to reduce 'throws' for getters
    private String versionName;
    
    private Date lastModified;
    private String modifiedBy;

    private int major;
    private int minor;
    private long revision;

    protected static void create(Node node) throws RepositoryException {
        node.setProperty(JcrNT.PROP_VERSION, 0);
        node.setProperty(JcrNT.PROP_REVISION, 0);
    }
    
    public JcrVersion(Node node) {
        initVersion(node);
    }
    
    public JcrVersion(Version version) throws RepositoryException {
        // storing node's properties into variables to reduce 'throws' for getters
        versionName = version.getName();
        Node frozen = version.getNode("jcr:frozenNode");
        
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
        major = version.getMajor();
        minor = version.getMinor();
        revision = version.getRevision();
    }

    public String getName() {
        if (versionName == null) {
            StringBuilder sb = new StringBuilder(8);
            sb.append(major);
            sb.append('.');
            sb.append(minor);
            sb.append('.');
            sb.append(revision);
            
            versionName = sb.toString();
        }
        
        return versionName;
    }

    public Date getCreated() {
        return lastModified;
    }

    public RUser getCreatedBy() {
        return new JcrUser(modifiedBy);
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getRevision() {
        return (int)revision;
    }
    
    // --- protected
    
    protected void initVersion(Node node) {
        try {
            long l = node.getProperty(JcrNT.PROP_VERSION).getLong();
            int i = (int)l;
            major = i >> 16;
            minor = i & (0xFFFF);
        } catch (RepositoryException e) {
            // TODO: add logging
            major = 0;
            minor = 0;
        }

        try {
            revision = node.getProperty(JcrNT.PROP_REVISION).getLong();
        } catch (RepositoryException e) {
            // TODO: add logging
            revision = 0;
        }
    }
    
    protected void nextRevision() {
        // only project can call this method
        revision++;
    }
    
    protected void updateVersion(Node node) throws RepositoryException {
        long l = (major << 16) | (minor & 0xFFFF);
        node.setProperty(JcrNT.PROP_VERSION, l);
        node.setProperty(JcrNT.PROP_REVISION, revision);
    }
    
    protected void updateRevision(Node node) throws RepositoryException {
        node.setProperty(JcrNT.PROP_REVISION, revision);
    }
}
