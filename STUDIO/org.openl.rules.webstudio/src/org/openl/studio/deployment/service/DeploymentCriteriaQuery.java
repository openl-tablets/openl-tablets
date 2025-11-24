package org.openl.studio.deployment.service;


import java.util.function.Predicate;

import org.openl.rules.project.abstraction.Deployment;
import org.openl.util.StringUtils;

/**
 * Criteria query for filtering deployments.
 *
 * @param repository the repository ID to filter by (optional)
 * @param name       the deployment name to filter by (optional)
 */
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String repository;
        private String name;

        public Builder repository(String repository) {
            this.repository = repository;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public DeploymentCriteriaQuery build() {
            return new DeploymentCriteriaQuery(repository, name);
        }
    }

}
