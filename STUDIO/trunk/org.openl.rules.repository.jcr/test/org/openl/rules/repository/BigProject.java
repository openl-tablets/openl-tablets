package org.openl.rules.repository;

public class BigProject {
    public static final String PROJECT_NAME = "big-project";

    public static void main(String[] args) {
        RRepository repository = null;
        CommonUser user = new CommonUserImpl("big");

        try {
            repository = RulesRepositoryFactory.getRepositoryInstance();
            if (!repository.hasProject(PROJECT_NAME)) {
                System.out.println("> No big project detected. Trying to create test set...");

                RProject prj = repository.createProject(PROJECT_NAME);
                prj.commit(user);
            } else {
                System.out.println("> Has big project");

                System.out.println("> Recreate...");
                RProject prj = repository.getProject(PROJECT_NAME);
                prj.erase(user);

                prj = repository.createProject(PROJECT_NAME);
                prj.commit(user);
            }

            RProject prj = repository.getProject(PROJECT_NAME);
            RFolder root = prj.getRootFolder();

            int count = 100;
            // int count = 10;
            System.out.println("> Adding " + count + " folder to project");

            for (int i = 0; i < count; i++) {
                String name = "f-" + i;

                System.out.println("+ folder " + name + "... " + Runtime.getRuntime().freeMemory());

                root.createFolder(name);
            }

            prj.commit(user);

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
