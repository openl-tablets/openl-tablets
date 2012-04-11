package org.openl.rules.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.openl.rules.repository.exceptions.RRepositoryException;

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
            CommonUser user = new CommonUserImpl("unknown");

            repository = RulesRepositoryFactory.getRepositoryInstance();
            if (repository.getProjects().size() == 0) {
                System.out.println("> No projects detected. Trying to create test set...");

                RProject prj1 = repository.createProject("prj1");
                repository.createProject("prj2");
                repository.createProject("prj3(marked)").delete(user);

                RFolder r1 = prj1.getRootFolder().createFolder("rules");

                r1.createFile("test1.txt");
                r1.createFile("test2.txt");
                prj1.commit(user);
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
            RFile f1 = p1.getRootFolder().getFolders().get(0).getFiles().get(0);
            if (f1.getVersionHistory().size() < 10) {
                // add 1 more version each launch
                int rev = p1.getActiveVersion().getRevision();

                if (rev % 2 == 0) {
                    String s = "updated-" + (rev);
                    f1.setContent(new java.io.ByteArrayInputStream(s.getBytes()));
                }
            }
            p1.commit(user);

            List<RVersion> p1vs = p1.getVersionHistory();
            System.out.println("> versions for /prj1 -- " + p1vs.size());
            for (RVersion v : p1vs) {
                System.out.println("  " + v.getVersionName() + " " + v.getCreated() + " by "
                        + v.getCreatedBy().getUserName());
            }

            List<RVersion> f1vs = f1.getVersionHistory();
            System.out.println("> versions for /prj1/rules/test1.txt -- " + f1vs.size());
            for (RVersion v : f1vs) {
                System.out.println("  " + v.getVersionName() + " " + v.getCreated() + " by "
                        + v.getCreatedBy().getUserName());
            }

            RVersion last = f1vs.get(f1vs.size() - 1);
            System.out.println("> checking content of version " + last.getVersionName());

            showFile4Version(f1, last);
            System.out.println("> checked");

        } catch (Exception e) {
            System.err.println("*** Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (repository != null) {
                repository.release();
            }
        }
    }

    private static void showFile4Version(RFile file, CommonVersion version) throws RRepositoryException, IOException {
        InputStream is = file.getContent4Version(version);

        StringBuilder sb = new StringBuilder(32);
        while (true) {
            int i = is.read();
            if (i < 0) {
                break;
            }

            sb.append((char) i);
        }

        is.close();

        System.out.println("  >" + sb.toString() + "<");
    }
}
