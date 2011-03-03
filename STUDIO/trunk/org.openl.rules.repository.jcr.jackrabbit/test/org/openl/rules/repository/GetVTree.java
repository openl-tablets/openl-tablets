package org.openl.rules.repository;

import static java.lang.System.out;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;

import org.openl.rules.common.ProjectDependency;
import org.openl.rules.repository.exceptions.RRepositoryException;

public class GetVTree {
    public static void main(String[] args) {
        RRepository repository = null;
        // CommonUser user = new CommonUserImpl("user1");

        try {
            repository = RulesRepositoryFactory.getRepositoryInstance();

            RProject prj = repository.getProject("p1");

            List<RVersion> vers = prj.getVersionHistory();
            out.println("Project versions: " + vers.size());
            for (RVersion v : vers) {
                out.println("  " + v.getVersionName());

                RProject old = prj.getProjectVersion(v);

                printProject(old);
            }

            out.println("Project Tree:");
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

    private static void printContent(RFile file) throws RRepositoryException, IOException {
        InputStream content = null;

        try {
            content = file.getContent();

            BufferedReader reader = new BufferedReader(new InputStreamReader(content));
            while (reader.ready()) {
                String s = reader.readLine();
                out.println("   >" + s);
            }
        } finally {
            if (content != null) {
                content.close();
            }
        }
    }

    private static void printProject(RProject prj) throws RRepositoryException, IOException {
        out.println("  P " + prj.getPath() + "    ver=" + prj.getActiveVersion().getVersionName() + " lob="
                + prj.getLineOfBusiness());

        Collection<ProjectDependency> deps = prj.getDependencies();
        for (ProjectDependency d : deps) {
            out.println("  d " + d.getProjectName() + " " + d.getLowerLimit().getVersionName());
        }

        printTree(prj.getRootFolder());
    }

    private static void printTree(RFolder folder) throws RRepositoryException, IOException {
        out.println("  F " + folder.getPath() + "    ver=" + folder.getActiveVersion().getVersionName() + " lob="
                + folder.getLineOfBusiness());

        List<RFolder> folders = folder.getFolders();
        for (RFolder f : folders) {
            printTree(f);
        }

        List<RFile> files = folder.getFiles();
        for (RFile f : files) {
            out.println("  R " + f.getPath() + "    ver=" + f.getActiveVersion().getVersionName() + " lob="
                    + f.getLineOfBusiness());

            printContent(f);
        }
    }
}
