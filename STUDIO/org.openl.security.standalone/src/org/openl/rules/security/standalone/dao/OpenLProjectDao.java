package org.openl.rules.security.standalone.dao;

import java.util.List;

import org.openl.rules.security.standalone.persistence.OpenLProject;
import org.openl.rules.security.standalone.persistence.Tag;
import org.springframework.transaction.annotation.Transactional;

public interface OpenLProjectDao extends Dao<OpenLProject> {
    @Transactional
    OpenLProject getById(Long id);

    @Transactional
    OpenLProject getProject(String repoId, String projectPath);

    @Transactional
    List<Tag> getTagsForProject(String repoId, String projectPath);

    @Transactional
    void deleteById(Long id);

    @Transactional
    List<OpenLProject> getProjectsForTag(Long id);
}
