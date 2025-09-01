package org.openl.rules.rest.deployment.model;

import java.util.List;

import org.openl.rules.rest.model.ProjectIdModel;

public class DeploymentViewModel {

    public final ProjectIdModel id;
    public final String name;
    public final String repository;
    public final List<DeploymentItemViewModel> items;

    public DeploymentViewModel(Builder from) {
        this.id = from.id;
        this.name = from.name;
        this.repository = from.repository;
        this.items = from.items;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        public ProjectIdModel id;
        private String name;
        private String repository;
        private List<DeploymentItemViewModel> items;

        public Builder id(ProjectIdModel id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder repository(String repository) {
            this.repository = repository;
            return this;
        }

        public Builder items(List<DeploymentItemViewModel> items) {
            this.items = items;
            return this;
        }

        public DeploymentViewModel build() {
            return new DeploymentViewModel(this);
        }
    }

}
