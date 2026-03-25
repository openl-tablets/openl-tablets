package org.openl.studio.deployment.service;


import java.util.function.Predicate;

import lombok.Builder;

import org.openl.rules.project.abstraction.Deployment;
import org.openl.util.StringUtils;

/**
 * Criteria query for filtering deployments.
 *
 * @param repository the repository ID to filter by (optional)
 * @param name       the deployment name to filter by (optional)
 */
@Builder
public record DeploymentCriteriaQuery(String repository, String name) {

    public Predicate<Deployment> getFilter() {
        Predicate<Deployment> filter = d -> true;
        if (StringUtils.isNotBlank(repository)) {
            filter = filter.and(d -> d.getRepository().getId().equals(repository));
        }
        if (StringUtils.isNotBlank(name)) {
            filter = filter.and(d -> d.getName().equalsIgnoreCase(name));
        }
        return filter;
    }

}
