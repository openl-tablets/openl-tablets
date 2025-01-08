package org.openl.rules.webstudio.security;

import java.util.List;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.DesignTimeRepository;

public interface SecureDesignTimeRepository extends DesignTimeRepository {

    List<Repository> getManageableRepositories();

    List<AProject> getManageableProjects();

}
