package org.openl.rules.repository.jcr;

import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Defines interface for JCR Entities that can have versions.
 * (Project, Folder, File).
 * 
 * @author Aleh Bykhavets
 *
 */
public interface JcrVersionable {
	/**
	 * Lists all versions of a JCR Entity.
	 * Return list keeps versions in chronological order.
	 * I.e. oldest version is first in list, when the latest is last.
	 * 
	 * @return list of versions
	 * @throws RepositoryException
	 */
	public List<JcrVersion> getVersions() throws RepositoryException;
}
