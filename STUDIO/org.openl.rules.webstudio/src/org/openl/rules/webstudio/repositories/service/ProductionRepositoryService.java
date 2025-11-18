package org.openl.rules.webstudio.repositories.service;

import java.util.List;

import org.openl.rules.rest.model.RepositoryViewModel;

public interface ProductionRepositoryService {

    List<RepositoryViewModel> getRepositoryList();
}
