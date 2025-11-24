package org.openl.studio.repositories.service;

import java.io.IOException;
import java.util.List;

import org.openl.rules.repository.api.Repository;
import org.openl.studio.repositories.model.RepositoryFeatures;
import org.openl.studio.repositories.model.RepositoryViewModel;

public interface DesignTimeRepositoryService {

    List<RepositoryViewModel> getRepositoryList();

    List<String> getBranches(String id) throws IOException;

    List<String> getBranches(Repository repository) throws IOException;

    RepositoryFeatures getFeatures(String id);

    RepositoryFeatures getFeatures(Repository repository);

}
