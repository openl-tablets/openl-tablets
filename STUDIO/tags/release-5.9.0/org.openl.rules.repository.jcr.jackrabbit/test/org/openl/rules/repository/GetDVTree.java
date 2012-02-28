package org.openl.rules.repository;

import static java.lang.System.out;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.openl.rules.repository.exceptions.RRepositoryException;

public class GetDVTree {
    public static void main(String[] args) {
        RRepository repository = null;
        // CommonUser user = new CommonUserImpl("user1");

        try {
            repository = RulesRepositoryFactory.getRepositoryInstance();

            RDeploymentDescriptorProject prj = repository.getDDProject("ddp1");

            List<RVersion> vers = prj.getVersionHistory();
            out.println("DProject versions: " + vers.size());
            for (RVersion v : vers) {
                out.println("  " + v.getVersionName());

                RDeploymentDescriptorProject old = prj.getProjectVersion(v);

                printProject(old);
            }

            out.println("DProject NOW:");
            printProject(prj);

        } catch (Exception e) {
            System.err.println("*** Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (repository != null) {
                repository.release();
            }
        }
    }

    private static void printProject(RDeploymentDescriptorProject prj) throws RRepositoryException, IOException {
        out.println("  P " + prj.getName() + "    ver=" + prj.getActiveVersion().getVersionName());

        Collection<RProjectDescriptor> descrs = prj.getProjectDescriptors();
        for (RProjectDescriptor p : descrs) {
            out.println("  p " + p.getProjectName() + " " + p.getProjectVersion().getVersionName());
        }
    }
}
