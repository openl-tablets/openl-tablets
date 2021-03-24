package org.openl.rules.security.standalone.dao;

import org.openl.rules.security.standalone.persistence.ProjectGrouping;
import org.springframework.transaction.annotation.Transactional;

public interface ProjectGroupingDao extends Dao<ProjectGrouping> {
    @Transactional
    ProjectGrouping getByLogin(String login);
}
