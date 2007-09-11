package org.openl.rules.repository.jcr.impl;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.openl.rules.repository.jcr.JcrNT;
import org.openl.rules.repository.jcr.JcrVersion;

/**
 * Implements JCR Version.
 * 
 * @author Aleh Bykhavets
 *
 */
public class JcrVersionImpl implements JcrVersion{
	/** JCR Version */
	private Version version;
	
	// temporary variables, just to reduce 'throws' for getters
	private String versionName;
	private Date lastModified;
	private String modifiedBy;
	
	public JcrVersionImpl(Version version) throws RepositoryException {
		this.version = version;

		// storing node's properties into variables to reduce 'throws' for getters
		versionName = version.getName();
		Node frozen = version.getNode("jcr:frozenNode");
		
		lastModified = version.getProperty("jcr:created").getDate().getTime();
//		if (frozen.hasProperty(JcrNT.PROP_MODIFIED_TIME)) {
//			lastModified = frozen.getProperty(JcrNT.PROP_MODIFIED_TIME).getDate().getTime();
//		}
		if (frozen.hasProperty(JcrNT.PROP_MODIFIED_BY)) { 
			modifiedBy = frozen.getProperty(JcrNT.PROP_MODIFIED_BY).getString();
		}
	}
	
    /** {@inheritDoc} */
	public String getVersionName() {
		return versionName;
	}

    /** {@inheritDoc} */
	public Date getLastModified() {
		return lastModified;
	}

    /** {@inheritDoc} */
	public String getModifiedBy() {
		return modifiedBy;
	}
}
