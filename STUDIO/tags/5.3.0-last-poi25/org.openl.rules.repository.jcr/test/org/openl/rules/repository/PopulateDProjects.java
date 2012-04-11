package org.openl.rules.repository;

import java.util.Date;
import java.util.List;

public class PopulateDProjects {
    private static class RV2 implements RVersion {
        private CommonVersion version;

        private RV2(int major, int minor, int revision) {
            version = new CommonVersionImpl(major, minor, revision);
        }

        public int compareTo(CommonVersion o) {
            return new CommonVersionImpl(this).compareTo(o);
        }

        public Date getCreated() {
            return null;
        }

        public CommonUser getCreatedBy() {
            return null;
        }

        public int getMajor() {
            return version.getMajor();
        }

        public int getMinor() {
            return version.getMinor();
        }

        public int getRevision() {
            return version.getRevision();
        }

        public String getVersionName() {
            return version.getVersionName();
        }

    }

    public static void main(String[] args) {
        RRepository repository = null;
        CommonUser user = new CommonUserImpl("unknown");

        try {
            repository = RulesRepositoryFactory.getRepositoryInstance();
            if (repository.getDDProjects().size() == 0) {
                System.out.println("> No deployments projects detected. Trying to create test set...");

                RDeploymentDescriptorProject dp1 = repository.createDDProject("ddp1");
                dp1.createProjectDescriptor("prj1").setProjectVersion(new RV2(0, 0, 10));
                dp1.createProjectDescriptor("prj2").setProjectVersion(new RV2(0, 0, 20));

                dp1.commit(user);
            } else {
                System.out.println("> Has some deployments projects");
            }

            List<RDeploymentDescriptorProject> projects = repository.getDDProjects();
            System.out.println("> OpenL Rules Projects: " + projects.size());
            for (RDeploymentDescriptorProject prj : projects) {
                System.out.println("  " + prj.getName() + " marked=" + prj.isMarked4Deletion() + " ver="
                        + prj.getActiveVersion().getVersionName());

                for (RProjectDescriptor descr : prj.getProjectDescriptors()) {
                    System.out.println("    " + descr.getProjectName() + " "
                            + descr.getProjectVersion().getVersionName());
                }
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
