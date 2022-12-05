package org.openl.security.acl.workspace;

import org.openl.rules.workspace.UserWorkspaceFactory;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;

public class SecureUserWorkspaceFactoryImpl implements UserWorkspaceFactory {
    private final UserWorkspaceFactory delegate;

    private final RepositoryAclService designRepositoryAclService;
    private final RepositoryAclService deployConfigRepositoryAclService;

    public SecureUserWorkspaceFactoryImpl(UserWorkspaceFactory userWorkspaceFactory,
            RepositoryAclService designRepositoryAclService,
            RepositoryAclService deployConfigRepositoryAclService) {
        this.delegate = userWorkspaceFactory;
        this.designRepositoryAclService = designRepositoryAclService;
        this.deployConfigRepositoryAclService = deployConfigRepositoryAclService;
    }

    @Override
    public UserWorkspace create(LocalWorkspaceManager localWorkspaceManager,
            DesignTimeRepository designTimeRepository,
            WorkspaceUser user) {
        UserWorkspace userWorkspace = delegate.create(localWorkspaceManager, designTimeRepository, user);
        return new SecureUserWorkspaceImpl(userWorkspace, designRepositoryAclService, deployConfigRepositoryAclService);
    }
}
