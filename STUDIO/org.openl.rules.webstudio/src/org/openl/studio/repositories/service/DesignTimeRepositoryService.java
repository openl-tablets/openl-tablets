package org.openl.studio.repositories.service;

import java.io.IOException;
import java.util.List;

import org.openl.rules.repository.api.Repository;
import org.openl.studio.repositories.model.RepositoryViewModel;

public interface DesignTimeRepositoryService {

    List<RepositoryViewModel> getRepositoryList();

    /** Whether the user can create a project in at least one design repository (a copy's target). */
    boolean canCreateInAnyRepository();

    List<String> getBranches(Repository repository) throws IOException;

}
