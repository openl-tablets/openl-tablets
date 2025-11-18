package org.openl.rules.webstudio.repositories.service;

import java.io.IOException;
import java.util.List;

import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.model.RepositoryFeatures;
import org.openl.rules.rest.model.RepositoryViewModel;

public interface DesignTimeRepositoryService {

    List<RepositoryViewModel> getRepositoryList();

    List<String> getBranches(String id) throws IOException;

    List<String> getBranches(Repository repository) throws IOException;

    RepositoryFeatures getFeatures(String id);

    RepositoryFeatures getFeatures(Repository repository);

}
