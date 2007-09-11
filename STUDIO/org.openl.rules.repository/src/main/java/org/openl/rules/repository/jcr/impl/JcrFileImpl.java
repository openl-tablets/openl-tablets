package org.openl.rules.repository.jcr.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.openl.rules.repository.jcr.JcrFile;
import org.openl.rules.repository.jcr.JcrNT;

/**
 * Implementation of JCR File.
 * 
 * @author Aleh Bykhavets
 *
 */
public class JcrFileImpl extends JcrEntityImpl implements JcrFile {

	/**
	 * Creates a new instance of file.
	 * 
	 * @param parentNode parent node (files or folder)
	 * @param nodeName name of new node
	 * @return newly created instance
	 * @throws RepositoryException if fails
	 */
	protected static JcrFileImpl createFile(Node parentNode, String nodeName) throws RepositoryException {
		Node n = NodeUtil.createNode(parentNode, nodeName, JcrNT.NT_FILE, true);
		
		String mimeType = "text/plain";
		String encoding = "UTF-8";
		long lastModifiedTime = System.currentTimeMillis();
        Calendar lastModified = Calendar.getInstance ();
        lastModified.setTimeInMillis(lastModifiedTime);

        String content = "Dummy text file... lastModified=" + lastModified;
		
        //create the file node - see section 6.7.22.6 of the spec
        //create the mandatory child node - jcr:content
        Node resNode = n.addNode (JcrNT.PROP_RES_CONTENT, JcrNT.NT_RESOURCE);
        resNode.setProperty (JcrNT.PROP_RES_MIMETYPE, mimeType);
        resNode.setProperty (JcrNT.PROP_RES_ENCODING, encoding);
        resNode.setProperty (JcrNT.PROP_RES_DATA, new ByteArrayInputStream (content.getBytes()));
        resNode.setProperty (JcrNT.PROP_RES_LASTMODIFIED, lastModified);
		
        NodeUtil.smartCheckin(n);
		
		return new JcrFileImpl(n);
	}
	
	public JcrFileImpl(Node node) throws RepositoryException {
		super(node);

		checkNodeType(JcrNT.NT_FILE);
	}

    /** {@inheritDoc} */
	public InputStream getInputStream() {
		// TODO Auto-generated method stub
		return null;
	}

    /** {@inheritDoc} */
	public String getMimeType() {
		// TODO Auto-generated method stub
		return null;
	}

    /** {@inheritDoc} */
	public long getSize() {
		// TODO Auto-generated method stub
		return 0;
	}

    /** {@inheritDoc} */ 
	public void updateContent(InputStream inputStream) throws RepositoryException {
		Node n = node();
		n.checkout();

        Node resNode = n.getNode ("jcr:content");

		long lastModifiedTime = System.currentTimeMillis();
        Calendar lastModified = Calendar.getInstance ();
        lastModified.setTimeInMillis(lastModifiedTime);

        resNode.setProperty ("jcr:data", inputStream);
        resNode.setProperty ("jcr:lastModified", lastModified);
        
        n.setProperty (JcrNT.PROP_MODIFIED_TIME, lastModified);
        
        n.save();
        n.checkin();
	}
}
