package org.openl.rules.repository.jcr;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonUserImpl;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.api.ArtefactProperties;

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
        //node.setProperty(ArtefactProperties.PROP_VERSION, 0);
        long l = ((MAX_MM_INT & 0x7FFF) << 16) | (MAX_MM_INT & 0x7FFF);
        node.setProperty(ArtefactProperties.PROP_VERSION, l);
        
        node.setProperty(ArtefactProperties.PROP_REVISION, 0);
    }

    public JcrVersion(Node node) throws RepositoryException {
        // frozen node
        initVersion(node);

        Node parent = node.getParent();
        if (parent.hasProperty("jcr:created")) {
            lastModified = parent.getProperty("jcr:created").getDate().getTime();
        }
    }

    public JcrVersion(RVersion version) {
        this.version = new CommonVersionImpl(version);
        this.lastModified = version.getCreated();
        this.modifiedBy = version.getCreatedBy().getUserName();
    }

    public JcrVersion(Version version) throws RepositoryException {
        // storing node's properties into variables to reduce 'throws' for
        // getters
        Node frozen = version.getNode(JcrNT.FROZEN_NODE);

        initVersion(frozen);

        lastModified = version.getProperty("jcr:created").getDate().getTime();
    }

    @Override
    public int compareTo(CommonVersion o) {
        return version.compareTo(o);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof JcrVersion)) {
            return false;
        }

        return compareTo((JcrVersion) obj) == 0;
    }

    @Override
    public Date getCreated() {
        return lastModified;
    }

    @Override
    public CommonUser getCreatedBy() {
        if (modifiedBy == null) {
            return new CommonUserImpl("system");
        } else {
            return new CommonUserImpl(modifiedBy);
        }
    }
    // --- protected

    @Override
    public String getRevision() {
        return version.getRevision();
    }

    @Override
    public String getVersionName() {
        return version.getVersionName();
    }

    protected void initVersion(Node node) throws RepositoryException {
        long revision = 0;
        int major = 0;
        int minor = 0;

        try {
            if (node.hasProperty(ArtefactProperties.PROP_VERSION)) {
                long l = node.getProperty(ArtefactProperties.PROP_VERSION).getLong();
                int i = (int) l;
                major = i >> 16;
                minor = i & (0xFFFF);
            } else {
                major = MAX_MM_INT;
                minor = MAX_MM_INT;
            }
        } catch (Exception e) {
            major = MAX_MM_INT;
            minor = MAX_MM_INT;
        }
        
        try {
            revision = node.getProperty(ArtefactProperties.PROP_REVISION).getLong();
        } catch (RepositoryException e) {
            // ignore
        }

        version = new CommonVersionImpl(major, minor, (int) revision);

        if (node.hasProperty(ArtefactProperties.PROP_MODIFIED_BY)) {
            modifiedBy = node.getProperty(ArtefactProperties.PROP_MODIFIED_BY).getString();
        }
    }

    protected void nextRevision() {
        // only project can call this method
        int newRevision = Integer.parseInt(version.getRevision());
        newRevision++;

        version = new CommonVersionImpl(newRevision);
    }

    protected void set() {
        // keep revision unchanged
        version = new CommonVersionImpl(version.getRevision());
    }

    protected void set(int revision) {
        version = new CommonVersionImpl(revision);
    }

    protected void updateRevision(Node node) throws RepositoryException {
        node.setProperty(ArtefactProperties.PROP_REVISION, version.getRevision());
    }

    protected void updateVersion(Node node) throws RepositoryException {
        if (node.hasProperty(ArtefactProperties.PROP_VERSION)) {
            long l = ((MAX_MM_INT & 0x7FFF) << 16) | (MAX_MM_INT & 0x7FFF);
            node.setProperty(ArtefactProperties.PROP_VERSION, l);
        }

        node.setProperty(ArtefactProperties.PROP_REVISION, version.getRevision());
    }
    
    @Override
    public int getMajor() {
        return version.getMajor();
    }

    @Override
    public int getMinor() {
        return version.getMinor();
    }
}
