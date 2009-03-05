package org.openl.rules.repository;

public class LockProject {
    public static void main(String[] args) {
        RRepository repository = null;
        CommonUser user = new CommonUserImpl("user4");

        try {
            repository = RulesRepositoryFactory.getRepositoryInstance();
            if (!repository.hasProject("prj4")) {
                System.out.println("> No prj4 detected. Trying to create test set...");

                RProject prj = repository.createProject("prj4");
                prj.commit(user);

            } else {
                System.out.println("> Has prj4 project");
            }

            RProject prj = repository.getProject("prj4");
            RLock lock = prj.getLock();
            showLock(lock);

            if (lock.isLocked()) {
                System.out.println("- unlocking...");
                prj.unlock(user);
            } else {
                System.out.println("- locking...");
                prj.lock(user);
            }

            showLock(lock);

        } catch (Exception e) {
            System.err.println("*** Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (repository != null) {
                repository.release();
            }
        }
    }

    private static void showLock(RLock lock) {
        System.out.println(" isLocked " + lock.isLocked());
        System.out.println(" lockedAt " + lock.getLockedAt());

        String lockedBy = (lock.isLocked()) ? lock.getLockedBy().getUserName() : null;
        System.out.println(" lockedBy " + lockedBy);
    }
}
