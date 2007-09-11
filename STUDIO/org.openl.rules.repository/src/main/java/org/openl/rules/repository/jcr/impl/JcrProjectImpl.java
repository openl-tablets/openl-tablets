package org.openl.rules.repository.jcr.impl;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.openl.rules.repository.jcr.JcrFolder;
import org.openl.rules.repository.jcr.JcrNT;
import org.openl.rules.repository.jcr.JcrProject;

/**
 * Implementation for JCR Project.
 * 
 * @author Aleh Bykhavets
 *
 */
public class JcrProjectImpl extends JcrEntityImpl implements JcrProject {
	//TODO: candidate to move into JcrNT
	private static final String NODE_FILES = "files";

	/** Project's root folder or project->files. */
	private JcrFolderImpl rootFolder;
	
	/**
	 * Creates new project instance.
	 * <p>
	 * Note that OpenL project cannot be created inside other OpenL project.
	 * I.e. nesting is not allowed for OpenL projects.
	 * 
	 * @param parentNode parent node
	 * @param nodeName name of node
	 * @return newly created project
	 * @throws RepositoryException if fails
	 */
	protected static JcrProjectImpl createProject(Node parentNode, String nodeName) throws RepositoryException {
		Node n = NodeUtil.createNode(parentNode, nodeName, JcrNT.NT_PROJECT, true);

		// set the same
		n.setProperty(JcrNT.PROP_PRJ_NAME, nodeName);
		// TODO what should be in default description?
		n.setProperty(JcrNT.PROP_PRJ_DESCR, "created " + new Date() + " by UNKNOWN");
		// TODO what should be in status?
		n.setProperty(JcrNT.PROP_PRJ_STATUS, "DRAFT");

		Node files = n.addNode(NODE_FILES, JcrNT.NT_FILES);
		files.addMixin(JcrNT.MIX_VERSIONABLE);
		
		parentNode.save();
		n.checkin();
		files.checkout();

		// TODO should we add default folders?
		JcrFolderImpl.createFolder(files, "bin");
		JcrFolderImpl.createFolder(files, "build");
		JcrFolderImpl.createFolder(files, "docs");
		JcrFolderImpl.createFolder(files, "rules");
		
		n.save();
		files.checkin();
		
		// TODO set Changed and ActiveProject
		
		return new JcrProjectImpl(n);
	}

	public JcrProjectImpl(Node node) throws RepositoryException {
		super(node);
		
		checkNodeType(JcrNT.NT_PROJECT);

		Node files = node.getNode(NODE_FILES);
		rootFolder = new JcrFolderImpl(files);
	}

    /** {@inheritDoc} */
	public JcrFolder getRootFolder() {
		return rootFolder;
	}

    /** {@inheritDoc} */
	public boolean isMarked4Deletion() throws RepositoryException {
		boolean isMarked;
		
		Node n = node();
		// even if property itself is 'false' it still means that project is 'marked'
		isMarked = n.hasProperty(JcrNT.PROP_PRJ_MARKED_4_DELETION);
		
		return isMarked;
	}
	
    /** {@inheritDoc} */
	public void mark4deletion() throws RepositoryException {
		Node n = node();
		
		n.checkout();
		n.setProperty(JcrNT.PROP_PRJ_MARKED_4_DELETION, true);
		n.save();
		n.checkin();
	}
	
    /** {@inheritDoc} */
	public void unmark4deletion() throws RepositoryException {
		Node n = node();
		
		n.checkout();
		n.setProperty(JcrNT.PROP_PRJ_MARKED_4_DELETION, (Value)null, PropertyType.BOOLEAN);
		n.save();
		n.checkin();
	}
}
