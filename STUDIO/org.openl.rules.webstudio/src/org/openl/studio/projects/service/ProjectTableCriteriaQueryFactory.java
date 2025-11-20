package org.openl.studio.projects.service;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import org.openl.util.StringUtils;

@Component
public class ProjectTableCriteriaQueryFactory {

    private static final String PROPERTIES_PREFIX = "properties.";

    public ProjectTableCriteriaQuery build(Map<String, String> params,
                                           Set<String> kinds,
                                           String name) {
        var queryBuilder = ProjectTableCriteriaQuery.builder().kinds(kinds).name(name);

        params.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(PROPERTIES_PREFIX))
                .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
                .forEach(entry -> {
                    var tag = entry.getKey().substring(PROPERTIES_PREFIX.length());
                    queryBuilder.property(tag, entry.getValue());
                });
        return  queryBuilder.build();
    }

}
