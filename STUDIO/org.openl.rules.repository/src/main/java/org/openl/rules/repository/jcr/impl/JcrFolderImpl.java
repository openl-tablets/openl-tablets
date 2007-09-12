package org.openl.rules.repository.jcr.impl;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.openl.rules.repository.jcr.JcrEntity;
import org.openl.rules.repository.jcr.JcrFile;
import org.openl.rules.repository.jcr.JcrFolder;
import org.openl.rules.repository.jcr.JcrNT;

/**
 * Implementation for JCR Folder.
 * 
 * @author Aleh Bykhavets
 *
 */
public class JcrFolderImpl extends JcrEntityImpl implements JcrFolder {

	/**
	 * Creates new folder.
	 * 
	 * @param parentNode parent node (files or other folder)
	 * @param nodeName name of new node
	 * @return newly created folder
	 * @throws RepositoryException if fails
	 */
	protected static JcrFolderImpl createFolder(Node parentNode, String nodeName) throws RepositoryException {
		Node n = NodeUtil.createNode(parentNode, nodeName, JcrNT.NT_FOLDER, true);

		NodeUtil.smartCheckin(n);

		return new JcrFolderImpl(n);
	}
	
	public JcrFolderImpl(Node node) throws RepositoryException {
		super(node);
		
		checkNodeType(JcrNT.NT_FOLDER);
	}

    /** {@inheritDoc} */
	public List<JcrFile> listFiles() throws RepositoryException {
		List<JcrFile> result = new LinkedList<JcrFile>();
		listNodes(result, true);
		return result;
	}

    /** {@inheritDoc} */
	public List<JcrFolder> listSubFolders() throws RepositoryException {
		List<JcrFolder> result = new LinkedList<JcrFolder>();
		listNodes(result, false);
		return result;
	}

    /** {@inheritDoc} */
	public boolean hasSubFolder(String name) throws RepositoryException {
		return hasNode(name, JcrNT.NT_FOLDER);
	}

    /** {@inheritDoc} */
	public JcrFolder getSubFolder(String name) throws RepositoryException {
		Node n = node().getNode(name);
		return new JcrFolderImpl(n);
	}

    /** {@inheritDoc} */
	public boolean hasFile(String name) throws RepositoryException {
		return hasNode(name, JcrNT.NT_FILE);
	}

    /** {@inheritDoc} */
	public JcrFile getFile(String name) throws RepositoryException {
		Node n = node().getNode(name);
		return new JcrFileImpl(n);
	}

    /** {@inheritDoc} */
	public JcrFolder createSubFolder(String name) throws RepositoryException {
		return JcrFolderImpl.createFolder(node(), name);
	}

    /** {@inheritDoc} */
	public JcrFile createFile(String name) throws RepositoryException {
		return JcrFileImpl.createFile(node(), name);
	}
	
	// ------ private methods ------
	
	/**
	 * Lists nodes.
	 * 
	 * @param list2add list to which nodes should be added
	 * @param isFiles whether return only files or only folders
	 * @throws RepositoryException
	 */
	private void listNodes(List list2add, boolean isFiles) throws RepositoryException {
		NodeIterator ni = node().getNodes();
		while (ni.hasNext()) {
			Node n = ni.nextNode();
			
			// TODO: use search? But looking through direct child nodes seems faster
			if (n.isNodeType(JcrNT.NT_FOLDER)) {
				if (!isFiles) {
					list2add.add(new JcrFolderImpl(n));
				}
			} else {
				if (isFiles) {
					list2add.add(new JcrFileImpl(n));
				}
			}
		}
	}
	
	/**
	 * Inquire whether the folder has child node with particular name and type. 
	 * 
	 * @param name name of node
	 * @param nodeType node type
	 * @return <code>true</code> if such child node exists; <code>false</code> otherwise
	 * @throws RepositoryException
	 */
	//TODO: may be nodeType is excessive; usually folder cannot contain sub folder and file with the same names 
	private boolean hasNode(String name, String nodeType) throws RepositoryException {
		// is it exist?
		boolean result = node().hasNode(name);

		if (result) {
			// is it of 'nodeType'?
			Node n = node().getNode(name);
			result = n.isNodeType(nodeType);
		}
		
		return result;
	}
}
