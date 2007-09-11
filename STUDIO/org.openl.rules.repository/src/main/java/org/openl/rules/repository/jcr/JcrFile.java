package org.openl.rules.repository.jcr;

import java.io.InputStream;

import javax.jcr.RepositoryException;

/**
 * Defines JCR File.
 * 
 * @author Aleh Bykhavets
 *
 */
public interface JcrFile extends JcrEntity, JcrVersionable {
	//TODO fields -- to be clarified
	public long getSize();
	public InputStream getInputStream();
	public String getMimeType();
	public void updateContent(InputStream inputStream) throws RepositoryException;
}
