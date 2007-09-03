package org.openl.rules.repository.ui;

import org.openl.rules.repository.ui.tree.TreeFile;
import org.openl.rules.repository.ui.tree.TreeFolder;
import org.openl.rules.repository.ui.tree.TreeProject;

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
	private TreeProject root;
	
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
			root = newProject("JCR Repository");
//			refreshProjects();
			initTestData();
		}

		return root;
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

		root.add(prj1).add(prj2).add(prj3);
	}
}
