package org.openl.rules.webstudio.service;

import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.dao.OpenLProjectDao;
import org.openl.rules.security.standalone.persistence.OpenLProject;
import org.openl.rules.security.standalone.persistence.Tag;

public class OpenLProjectService {

    private final OpenLProjectDao projectDao;

    public OpenLProjectService(OpenLProjectDao projectDao) {
        this.projectDao = projectDao;
    }

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

    @Transactional(readOnly = true)
    public boolean isProjectHasTags(String repoId, String projectPath, Map<String, String> tags) {
        return projectDao.isProjectHasTags(repoId, projectPath, tags);
    }

    @Transactional(readOnly = true)
    public List<Tag> getTagsForProject(String repoId, String projectPath) {
        return projectDao.getTagsForProject(repoId, projectPath);
    }

}
