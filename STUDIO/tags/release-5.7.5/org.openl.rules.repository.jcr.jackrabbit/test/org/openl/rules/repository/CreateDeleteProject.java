package org.openl.rules.repository;

import java.util.List;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.impl.CommonUserImpl;

public class CreateDeleteProject {
    public static void main(String[] args) {
        RRepository repository = null;
        CommonUser user = new CommonUserImpl("unknown");

        try {
            repository = RulesRepositoryFactory.getRepositoryInstance();
            if (repository.getProjects().size() == 0) {
                System.out.println("> No projects detected. Trying to create test set...");

                RProject prj1 = repository.createProject("prj1");
                prj1.commit(user);

                RProject prj2 = repository.createProject("prj2");
                prj2.getRootFolder().createFolder("f2");
                prj2.commit(user);

                RProject prj3 = repository.createProject("prj3");
                RFolder f3 = prj3.getRootFolder().createFolder("f3");
                f3.createFolder("f3-1");
                f3.createFolder("f3-2");
                prj3.commit(user);
            } else {
                System.out.println("> Has some projects");
            }

            RProject p2 = repository.getProject("prj2");
            if (p2.isMarked4Deletion()) {
                p2.undelete(user);
            } else {
                p2.delete(user);
            }
            // repository.getProject("prj3").erase();

            List<RProject> projects = repository.getProjects();
            System.out.println("> OpenL Rules Projects: " + projects.size());
            for (RProject prj : projects) {
                System.out.println("  " + prj.getName() + " marked=" + prj.isMarked4Deletion());
            }

            List<RProject> projects4del = repository.getProjects4Deletion();
            System.out.println("> Projects marked for deletion: " + projects4del.size());
            for (RProject prj : projects4del) {
                System.out.println("  " + prj.getName());
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
