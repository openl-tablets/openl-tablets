package org.openl.rules.webstudio.projects.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.rest.service.ProjectCriteriaQuery;
import org.openl.util.StringUtils;

@Component
public class ProjectCriteriaQueryFactory {

    private static final String TAGS_PREFIX = "tags.";

    public ProjectCriteriaQuery build(Map<String, String> params,
                                      ProjectStatus status,
                                      String repository) {
        var queryBuilder = ProjectCriteriaQuery.builder().repositoryId(repository).status(status);

        params.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(TAGS_PREFIX))
                .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
                .forEach(entry -> {
                    var tag = entry.getKey().substring(TAGS_PREFIX.length());
                    queryBuilder.tag(tag, entry.getValue());
                });
        return queryBuilder.build();
    }

}
