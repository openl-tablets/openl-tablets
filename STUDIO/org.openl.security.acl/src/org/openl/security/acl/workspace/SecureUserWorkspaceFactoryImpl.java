package org.openl.security.acl.workspace;

import org.openl.rules.workspace.UserWorkspaceFactory;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.DesignRepositoryAclService;

public class SecureUserWorkspaceFactoryImpl implements UserWorkspaceFactory {
    private final UserWorkspaceFactory delegate;

    private final DesignRepositoryAclService designRepositoryAclService;

    public SecureUserWorkspaceFactoryImpl(UserWorkspaceFactory userWorkspaceFactory,
            DesignRepositoryAclService designRepositoryAclService) {
        this.delegate = userWorkspaceFactory;
        this.designRepositoryAclService = designRepositoryAclService;
    }

    @Override
    public UserWorkspace create(LocalWorkspaceManager localWorkspaceManager,
            DesignTimeRepository designTimeRepository,
            WorkspaceUser user) {
        UserWorkspace userWorkspace = delegate.create(localWorkspaceManager, designTimeRepository, user);
        return new SecureUserWorkspaceImpl(userWorkspace, designRepositoryAclService);
    }
}
