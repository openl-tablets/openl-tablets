package org.openl.studio.repositories.service;

import java.util.List;

import org.openl.studio.repositories.model.RepositoryViewModel;

public interface DeploymentRepositoryService {

    List<RepositoryViewModel> getRepositoryList();

    /**
     * Whether the current user can deploy to at least one production repository — that is, has WRITE on a
     * deployment repository whose main branch is not protected. Mirrors the legacy Redeploy gate.
     */
    boolean canDeployToAnyRepository();
}
