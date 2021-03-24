package org.openl.rules.webstudio.service;

import org.openl.rules.security.standalone.dao.ProjectGroupingDao;
import org.openl.rules.security.standalone.persistence.ProjectGrouping;

public class ProjectGroupingService {

    private ProjectGroupingDao projectGroupingDao;

    public ProjectGrouping getProjectGrouping(String login) {
        return projectGroupingDao.getByLogin(login);
    }

    public void save(ProjectGrouping projectGrouping) {
        final ProjectGrouping grouping = projectGroupingDao.getByLogin(projectGrouping.getLoginName());
        if (grouping == null) {
            projectGroupingDao.save(projectGrouping);
        } else {
            projectGroupingDao.update(projectGrouping);
        }
    }

    public void setProjectGroupingDao(ProjectGroupingDao projectGroupingDao) {
        this.projectGroupingDao = projectGroupingDao;
    }
}
