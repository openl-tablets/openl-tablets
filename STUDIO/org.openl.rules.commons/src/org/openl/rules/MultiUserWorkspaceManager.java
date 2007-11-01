package org.openl.rules;

import org.openl.rules.commons.projects.ProjectException;
import org.openl.rules.dtr.DesignTimeRepository;
import org.openl.rules.dtr.impl.DesignTimeRepositoryImpl;
import org.openl.rules.lw.LocalWorkspace;
import org.openl.rules.lw.LocalWorkspaceManager;
import org.openl.rules.lw.impl.LocalWorkspaceManagerImpl;
import org.openl.rules.uw.UserWorkspace;
import org.openl.rules.uw.impl.UserWorkspaceImpl;

import java.util.HashMap;
import java.util.Map;

public class MultiUserWorkspaceManager {
    private LocalWorkspaceManager localManager;
    private DesignTimeRepository designTimeRepository;
    private Map<String, UserWorkspace> userWorkspaces;

    public MultiUserWorkspaceManager() throws WorkspaceException {
        userWorkspaces = new HashMap<String, UserWorkspace>();

        localManager = new LocalWorkspaceManagerImpl();
        designTimeRepository = new DesignTimeRepositoryImpl();
    }

    public UserWorkspace getUserWorkspace(WorkspaceUser user) throws WorkspaceException {
        UserWorkspace uw = userWorkspaces.get(user.getUserId());
        if (uw == null) {
            uw = createUserWorkspace(user);
            userWorkspaces.put(user.getUserId(), uw);
        }

        return uw;
    }

    protected UserWorkspace createUserWorkspace(WorkspaceUser user) throws WorkspaceException {
        LocalWorkspace lw = localManager.getWorkspace(user);
        UserWorkspace uw = new UserWorkspaceImpl(user, lw, designTimeRepository);
        return uw;
    }

    public static void main(String[] args) throws WorkspaceException, ProjectException {
        MultiUserWorkspaceManager muwm = new MultiUserWorkspaceManager();

        WorkspaceUser wu = new WorkspaceUserImpl("127.0.0.1");
        UserWorkspace uw = muwm.getUserWorkspace(wu);
        uw.activate();

        System.out.println(uw.getProjects().size());

//        pseudo-publish project in DTR
//        Project p = uw.getProject("prj1");
//        muwm.designTimeRepository.copyProject(p, "prj1");
    }
}
