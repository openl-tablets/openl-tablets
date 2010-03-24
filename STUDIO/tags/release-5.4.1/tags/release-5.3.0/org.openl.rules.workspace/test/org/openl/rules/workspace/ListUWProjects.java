package org.openl.rules.workspace;

import java.util.Collection;

import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.rules.workspace.uw.UserWorkspaceProjectArtefact;
import org.openl.rules.workspace.uw.UserWorkspaceProjectFolder;

public class ListUWProjects {
    public static void main(String[] args) throws WorkspaceException, ProjectException {
        MultiUserWorkspaceManager muwm = new MultiUserWorkspaceManager();

        WorkspaceUser wu = new WorkspaceUserImpl("127.0.0.1");
        UserWorkspace uw = muwm.getUserWorkspace(wu);
        uw.activate();

        Collection<UserWorkspaceProject> projects = uw.getProjects();
        System.out.println("> Listing rules project:" + projects.size());

        for (UserWorkspaceProject prj : projects) {
            System.out.println(prj.getName() + " " + prj.getVersion().getVersionName());
        }

        UserWorkspaceProject prj = projects.iterator().next();
        for (UserWorkspaceProjectArtefact a : prj.getArtefacts()) {
            printArtefact(a);
        }

        uw.passivate();
        System.out.println("Done.");
    }

    private static void printArtefact(UserWorkspaceProjectArtefact a) {
        String path = a.getArtefactPath().getStringValue();

        if (a.isFolder()) {
            System.out.println(" F " + path);

            UserWorkspaceProjectFolder folder = (UserWorkspaceProjectFolder) a;

            for (UserWorkspaceProjectArtefact fa : folder.getArtefacts()) {
                printArtefact(fa);
            }
        } else {
            System.out.println(" R " + path);
        }
    }
}
