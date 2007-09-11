package org.openl.rules.repository.jcr.impl;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.openl.rules.repository.RepositoryFactory;
import org.openl.rules.repository.jcr.JcrFile;
import org.openl.rules.repository.jcr.JcrProject;
import org.openl.rules.repository.jcr.JcrVersion;

/**
 * Populates JCR Repository with test data.
 * <p>
 * For internal use only.
 * 
 * @author Aleh Bykhavets
 *
 */
public class PopulateJcrRepository {
	/**
	 * Entry point for this java program.
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		Session session = null;
		
		JcrRepositoryImpl jri = null;
		try {
			jri = (JcrRepositoryImpl)RepositoryFactory.getRepositoryInstance();
			// TODO: not good, we should avoid getting session this way
			session = jri.getSession();
			
			Node root = session.getRootNode();
			Node n;
			if (!root.hasNode("TEST")) {
				System.out.println("> Create TEST");
				n = root.addNode("TEST");
				session.save();

				JcrProject prj1 = jri.createProject(n, "prj1");
				jri.createProject(n, "prj2");
				jri.createProject(n, "prj3(marked)").mark4deletion();
				
				JcrFolderImpl r1 = (JcrFolderImpl)prj1.getRootFolder().getSubFolder("rules");
				JcrFileImpl.createFile(r1.node(), "test1.txt");
				JcrFileImpl.createFile(r1.node(), "test2.txt");
				
				session.save();
			} else {
				System.out.println("> Has TEST");
//				n = root.getNode("TEST");
//				n.remove();
//				session.save();
			}

			List<JcrProject> projects = jri.listProjects();
			System.out.println("> OpenL Projects " + projects.size());

			List<JcrProject> projects4del = jri.listProjects4Deletion();
			System.out.println("> Projects 4 Deletion " + projects4del.size());
			
			JcrProject p1 = projects.get(0);
			JcrFile f1 = p1.getRootFolder().getSubFolder("rules").getFile("test1.txt");
//			f1.updateContent(new java.io.ByteArrayInputStream("updated+".getBytes()));
			
			List<JcrVersion> f1vs = f1.getVersions();
			System.out.println("> versions for /prj1/rules/test1.txt -- " + f1vs.size());
			for (JcrVersion v : f1vs) {
				System.out.println("  " + v.getVersionName() + " " + v.getLastModified());
			}
		} catch (Exception e) {
			System.err.println("*** Error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (session != null) {
				session.logout();
			}
		}
	}
}
