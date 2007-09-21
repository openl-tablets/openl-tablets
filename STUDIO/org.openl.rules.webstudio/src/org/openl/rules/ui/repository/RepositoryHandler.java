package org.openl.rules.ui.repository;

import java.util.LinkedList;
import java.util.List;

import org.openl.rules.ui.repository.beans.Element;
import org.openl.rules.ui.repository.tree.TreeFile;
import org.openl.rules.ui.repository.tree.TreeFolder;
import org.openl.rules.ui.repository.tree.TreeProject;
import org.openl.rules.ui.repository.tree.TreeRepository;

/**
 * Handler for Repository/Projects Tree
 * 
 * @author Aleh Bykhavets
 *
 */
public class RepositoryHandler {

//	private JcrRepository jcrRepository;
	
	/**
	 * Root node for RichFaces's tree.  It won't be displayed. 
	 */
	private TreeRepository root;
	
	public RepositoryHandler() {
//		jcrRepository = JcrRepositoryFactory.getRepositoryInstance();
	}
	
	@Override
	protected void finalize() throws Throwable {
//		jcrRepository.release();
		super.finalize();
	}
	
	public Object getData() {
		if (root == null) {
			root = new TreeRepository(generateId(), "JCR Repository");
//			refreshProjects();
			initTestData();
		}

		return root;
	}
	
	public List<Element> getProjects() {
		List<Element> result = new LinkedList<Element>();
		
		result.add(new Element("prj1", "1.2", "09/08/2007 10:32am", "John S."));
		result.add(new Element("prj2", "1.1", "09/05/2007  9:40am", "Alex T."));
		result.add(new Element("prj3", "1.1", "09/05/2007  9:40am", "Jonh S."));
		
		return result;
	}
	
//	protected void refreshProjects() throws RepositoryException {
//		// TODO: implement 'fast' refresh
//		root.clear();
//		
//		for (JcrProject project : jcrRepository.listProjects()) {
//			TreeProject treeNode = newProject(project.getName());
//			
//			refreshProject(treeNode, project);
//			
//			root.add(treeNode);
//		}
//	}
//	
//	protected void refreshProject(TreeProject treeNode, JcrProject project) throws RepositoryException {
//		JcrFolder f = project.getRootFolder();
//		refreshFolder(treeNode, f);
//	}
//	
//	protected void refreshFolder(TreeFolder treeNode, JcrFolder folder) throws RepositoryException {
//		for (JcrFolder f : folder.listSubFolders()) {
//			TreeFolder tf = newFolder(f.getName());
//			treeNode.add(tf);
//			
//			// recursion
//			refreshFolder(tf, f);
//		}
//		
//		for (JcrFile f : folder.listFiles()) {
//			TreeFile tf = newFile(f.getName());
//			treeNode.add(tf);
//		}
//	}
	
	// ------ private methods ------
	
	private static long lastId;
	
	private static synchronized long generateId() {
		return lastId++;
	}
	
	private TreeFile newFile(String name) {
		return new TreeFile(generateId(), name);
	}
	
	private TreeFolder newFolder(String name) {
		return new TreeFolder(generateId(), name);
	}

	private TreeProject newProject(String name) {
		return new TreeProject(generateId(), name);
	}
	
	private void initTestData() {
		TreeProject prj1 = newProject("prj1");
		TreeFolder f1 = newFolder("rules");
		f1.add(newFile("main.xls"));
		f1.add(newFile("inc.xls"));

		prj1.add(newFolder("bin"));
		prj1.add(newFolder("build"));
		prj1.add(newFolder("docs"));
		prj1.add(f1);
		
		TreeProject prj2 = newProject("prj2");
		prj2.add(newFolder("bin"));
		prj2.add(newFolder("build"));
		prj2.add(newFolder("docs"));
		prj2.add(newFolder("rules"));

		TreeProject prj3 = newProject("prj3");
		prj3.add(newFolder("bin"));
		prj3.add(newFolder("build"));
		prj3.add(newFolder("docs"));
		prj3.add(newFolder("rules"));

		TreeRepository vis = new TreeRepository(generateId(), "Repository");
		vis.add(prj1).add(prj2).add(prj3);
		root.add(vis);
	}
}
