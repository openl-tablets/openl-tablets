package org.openl.rules.repository;

import java.util.List;

/**
 * Populates OpenL Repository with test data.
 * <p>
 * For internal use only.
 * 
 * @author Aleh Bykhavets
 * 
 */
public class PopulateRepository {
    /**
     * Entry point for this java program.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        RRepository repository = null;
        try {
            repository = RulesRepositoryFactory.getRepositoryInstance();
            if (repository.getProjects().size() == 0) {
                System.out.println("> No projects detected. Trying to create test set...");

                RProject prj1 = repository.createProject("prj1");
                repository.createProject("prj2");
                repository.createProject("prj3(marked)").delete();

                RFolder r1 = prj1.getRootFolder().getFolders().get(3);
                r1.createFile("test1.txt");
                r1.createFile("test2.txt");
            } else {
                System.out.println("> Has some projects");
            }

            List<RProject> projects = repository.getProjects();
            System.out.println("> OpenL Rules Projects: " + projects.size());
            for (RProject prj : projects) {
                System.out.println("  " + prj.getName());
            }

            List<RProject> projects4del = repository.getProjects4Deletion();
            System.out.println("> Projects marked for deletion: " + projects4del.size());
            for (RProject prj : projects4del) {
                System.out.println("  " + prj.getName());
            }

            RProject p1 = projects.get(0);
            RFile f1 = p1.getRootFolder().getFolders().get(3).getFiles()
                    .get(0);
            if (f1.getVersionHistory().size() < 10) {
                // add 1 more version each launch
                f1.setContent(new java.io.ByteArrayInputStream("updated+".getBytes()));
            }

            List<RVersion> f1vs = f1.getVersionHistory();
            System.out.println("> versions for /prj1/rules/test1.txt -- " + f1vs.size());
            for (RVersion v : f1vs) {
                System.out.println("  " + v.getName() + " " + v.getCreated());
            }
        } catch (Exception e) {
            System.err.println("*** Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (repository != null) {
                repository.release();
            }
        }
    }
}
