package org.openl.studio.repositories.service;

import java.util.List;

import org.openl.studio.repositories.model.RepositoryViewModel;

public interface DeploymentRepositoryService {

    List<RepositoryViewModel> getRepositoryList();
}
