package org.openl.rules.workspace;

import java.util.Collection;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.workspace.uw.UserWorkspace;

public class ListUWProjects {
    public static void main(String[] args) throws WorkspaceException, ProjectException {
        MultiUserWorkspaceManager muwm = new MultiUserWorkspaceManager();

        WorkspaceUser wu = new WorkspaceUserImpl("127.0.0.1");
        UserWorkspace uw = muwm.getUserWorkspace(wu);
        uw.activate();

        Collection<AProject> projects = uw.getProjects();
        System.out.println("> Listing rules project:" + projects.size());

        for (AProject prj : projects) {
            System.out.println(prj.getName() + " " + prj.getVersion().getVersionName());
        }

        AProject prj = projects.iterator().next();
        for (AProjectArtefact a : prj.getArtefacts()) {
            printArtefact(a);
        }

        uw.passivate();
        System.out.println("Done.");
    }

    private static void printArtefact(AProjectArtefact a) {
        String path = a.getArtefactPath().getStringValue();

        if (a.isFolder()) {
            System.out.println(" F " + path);

            AProjectFolder folder = (AProjectFolder) a;

            for (AProjectArtefact fa : folder.getArtefacts()) {
                printArtefact(fa);
            }
        } else {
            System.out.println(" R " + path);
        }
    }
}
