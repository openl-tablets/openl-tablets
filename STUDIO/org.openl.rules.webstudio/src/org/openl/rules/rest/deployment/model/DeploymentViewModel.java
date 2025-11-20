package org.openl.rules.rest.deployment.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.studio.common.model.GenericView;
import org.openl.studio.projects.model.ProjectIdModel;

@Schema(description = "Deployment view model")
public class DeploymentViewModel {

    @JsonView({GenericView.Full.class, GenericView.Short.class})
    @Parameter(description = "Deployment identifier", required = true)
    public final ProjectIdModel id;

    @JsonView({GenericView.Full.class, GenericView.Short.class})
    @Parameter(description = "Deployment name", required = true)
    public final String name;

    @JsonView({GenericView.Full.class, GenericView.Short.class})
    @Parameter(description = "Production repository id", required = true)
    public final String repository;

    @JsonView(GenericView.Full.class)
    @Parameter(description = "List of deployment items")
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
