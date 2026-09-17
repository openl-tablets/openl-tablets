package org.openl.security.acl.workspace;

import lombok.RequiredArgsConstructor;

import org.openl.rules.workspace.UserWorkspaceFactory;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;

@RequiredArgsConstructor
public class SecureUserWorkspaceFactoryImpl implements UserWorkspaceFactory {
    private final UserWorkspaceFactory userWorkspaceFactory;
    private final RepositoryAclService designRepositoryAclService;

    @Override
    public UserWorkspace create(LocalWorkspaceManager localWorkspaceManager,
                                DesignTimeRepository designTimeRepository,
                                WorkspaceUser user) {
        var userWorkspace = userWorkspaceFactory.create(localWorkspaceManager, designTimeRepository, user);
        return new SecureUserWorkspaceImpl(userWorkspace, designRepositoryAclService);
    }
}
