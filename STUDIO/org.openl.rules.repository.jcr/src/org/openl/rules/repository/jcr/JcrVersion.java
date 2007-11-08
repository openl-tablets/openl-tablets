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
    /** JCR Version */
    private Version version;

    // temporary variables, just to reduce 'throws' for getters
    private String versionName;
    private Date lastModified;
    private String modifiedBy;

    public JcrVersion(Version version) throws RepositoryException {
        this.version = version;

        // storing node's properties into variables to reduce 'throws' for getters
        versionName = version.getName();
        Node frozen = version.getNode("jcr:frozenNode");

        lastModified = version.getProperty("jcr:created").getDate().getTime();
//      if (frozen.hasProperty(JcrNT.PROP_MODIFIED_TIME)) {
//      lastModified = frozen.getProperty(JcrNT.PROP_MODIFIED_TIME).getDate().getTime();
//      }
        if (frozen.hasProperty(JcrNT.PROP_MODIFIED_BY)) { 
            modifiedBy = frozen.getProperty(JcrNT.PROP_MODIFIED_BY).getString();
        }
    }

    /** {@inheritDoc} */
    public String getName() {
        return versionName;
    }

    /** {@inheritDoc} */
    public Date getCreated() {
        return lastModified;
    }

    /** {@inheritDoc} */
    public RUser getCreatedBy() {
        return new JcrUser(modifiedBy);
    }

    public int getMajor() {
        return 0;
    }

    public int getMinor() {
        return 0;
    }

    public int getRevision() {
        return 0;
    }
}
