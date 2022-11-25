package org.openl.security.acl.workspace;

import org.openl.rules.workspace.UserWorkspaceFactory;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;

public class SecureUserWorkspaceFactoryImpl implements UserWorkspaceFactory {
    private final UserWorkspaceFactory delegate;

    private final RepositoryAclService repositoryAclService;

    public SecureUserWorkspaceFactoryImpl(UserWorkspaceFactory userWorkspaceFactory,
            RepositoryAclService repositoryAclService) {
        this.delegate = userWorkspaceFactory;
        this.repositoryAclService = repositoryAclService;
    }

    @Override
    public UserWorkspace create(LocalWorkspaceManager localWorkspaceManager,
            DesignTimeRepository designTimeRepository,
            WorkspaceUser user) {
        UserWorkspace userWorkspace = delegate.create(localWorkspaceManager, designTimeRepository, user);
        return new SecureUserWorkspaceImpl(userWorkspace, repositoryAclService);
    }
}
