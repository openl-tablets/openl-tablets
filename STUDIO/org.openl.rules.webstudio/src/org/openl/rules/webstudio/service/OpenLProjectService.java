package org.openl.rules.webstudio.service;

import java.util.List;

import org.openl.rules.security.standalone.dao.OpenLProjectDao;
import org.openl.rules.security.standalone.persistence.OpenLProject;

public class OpenLProjectService {

    private OpenLProjectDao projectDao;

    public OpenLProject getProject(String repoId, String projectPath) {
        return projectDao.getProject(repoId, projectPath);
    }

    public void save(OpenLProject project) {
        projectDao.save(project);
    }

    public void update(OpenLProject tag) {
        projectDao.update(tag);
    }

    public void delete(Long id) {
        projectDao.deleteById(id);
    }

    public OpenLProject getById(Long id) {
        return projectDao.getById(id);
    }

    public List<OpenLProject> getProjectsForTag(Long id) {
        return projectDao.getProjectsForTag(id);
    }

    public void setProjectDao(OpenLProjectDao projectDao) {
        this.projectDao = projectDao;
    }
}
