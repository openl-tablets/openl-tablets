package org.openl.rules.security.standalone.dao;

import java.util.List;
import java.util.Map;

import org.openl.rules.security.standalone.persistence.OpenLProject;
import org.openl.rules.security.standalone.persistence.Tag;

public interface OpenLProjectDao extends Dao<OpenLProject> {

    OpenLProject getById(Long id);

    OpenLProject getProject(String repoId, String projectPath);

    List<Tag> getTagsForProject(String repoId, String projectPath);

    void deleteById(Long id);

    List<OpenLProject> getProjectsForTag(Long id);

    boolean isProjectHasTags(String repoId, String projectPath, Map<String, String> tags);
}
