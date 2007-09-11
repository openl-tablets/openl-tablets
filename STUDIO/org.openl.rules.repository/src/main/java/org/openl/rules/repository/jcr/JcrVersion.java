package org.openl.rules.repository.jcr;

import java.util.Date;

/**
 * Defines JCR Version.
 * 
 * @author Aleh Bykhavets
 *
 */
public interface JcrVersion {
	/**
	 * Gets name of the version.
	 * Usualy it returns '1.0', '1.1' and so on.
	 * <p>
	 * For the first version in a version history it returns 'jcr:rootVersion'.
	 * But root version has no data and so it has no use for us.
	 * 
	 * @return name of version
	 */
	public String getVersionName();
	
	/**
	 * Returns time when the version was modified last time.
	 * 
	 * @return date and time of last modification
	 */
	public Date getLastModified();
	
	/**
	 * Returns name(id?) of user who modified the version last time.
	 * I.e. who created this particular version.
	 * 
	 * @return name of user who created the version
	 */
	// TODO: clarify whether it should return name of user or one's id
	public String getModifiedBy();
}
