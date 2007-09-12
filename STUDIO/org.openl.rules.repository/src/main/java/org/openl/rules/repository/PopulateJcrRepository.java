package org.openl.rules.repository;

import java.util.List;

import org.openl.rules.repository.jcr.JcrFile;
import org.openl.rules.repository.jcr.JcrFolder;
import org.openl.rules.repository.jcr.JcrProject;
import org.openl.rules.repository.jcr.JcrRepository;
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
		JcrRepository jr = null;
		try {
			jr = RepositoryFactory.getRepositoryInstance();
			if (jr.listProjects().size() == 0) {
				System.out.println("> No projects detected. Trying to create test set...");

				JcrProject prj1 = jr.createProject("prj1");
				jr.createProject("prj2");
				jr.createProject("prj3(marked)").mark4deletion();
				
				JcrFolder r1 = prj1.getRootFolder().getSubFolder("rules");
				r1.createFile("test1.txt");
				r1.createFile("test2.txt");
			} else {
				System.out.println("> Has some projects");
			}

			List<JcrProject> projects = jr.listProjects();
			System.out.println("> OpenL Projects: " + projects.size());
			for (JcrProject prj : projects) {
				System.out.println("  " + prj.getName());
			}

			List<JcrProject> projects4del = jr.listProjects4Deletion();
			System.out.println("> Projects marked for deletion: " + projects4del.size());
			for (JcrProject prj : projects4del) {
				System.out.println("  " + prj.getName());
			}
			
			JcrProject p1 = projects.get(0);
			JcrFile f1 = p1.getRootFolder().getSubFolder("rules").getFile("test1.txt");
			if (f1.getVersions().size() < 10) {
				// add 1 more version each launch
				f1.updateContent(new java.io.ByteArrayInputStream("updated+".getBytes()));
			}
			
			List<JcrVersion> f1vs = f1.getVersions();
			System.out.println("> versions for /prj1/rules/test1.txt -- " + f1vs.size());
			for (JcrVersion v : f1vs) {
				System.out.println("  " + v.getVersionName() + " " + v.getLastModified());
			}
		} catch (Exception e) {
			System.err.println("*** Error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (jr != null) {
				jr.release();
			}
		}
	}
}
