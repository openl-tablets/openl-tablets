package org.openl.studio.projects.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.studio.common.model.GenericView;
import org.openl.studio.projects.converter.ProjectStatusSerializer;

@JsonPropertyOrder({"id"})
public abstract class AProjectViewModel {

    @Parameter(description = "Project Name", required = true)
    @JsonView(GenericView.Full.class)
    public final String name;

    @Parameter(description = "Branch Name. Can be absent if current repository doesn't support branches")
    @JsonView({GenericView.CreateOrUpdate.class, GenericView.Full.class})
    public final String branch;

    @Parameter(description = "Project identifier", required = true)
    @JsonView(GenericView.Full.class)
    public final ProjectIdModel id;

    @Parameter(description = "Project Status", schema = @Schema(allowableValues = {"LOCAL",
            "ARCHIVED",
            "OPENED",
            "VIEWING_VERSION",
            "EDITING",
            "CLOSED"}))
    @JsonSerialize(using = ProjectStatusSerializer.class)
    @JsonView(GenericView.Full.class)
    public final ProjectStatus status;

    @Parameter(description = "Source Repository")
    @JsonView(GenericView.Full.class)
    public final String repository;

    protected AProjectViewModel(ABuilder<?> from) {
        this.name = from.name;
        this.branch = from.branch;
        this.id = from.id;
        this.status = from.status;
        this.repository = from.repository;
    }

    public static abstract class ABuilder<T extends ABuilder<T>> {
        protected String name;
        protected String branch;
        protected ProjectIdModel id;
        protected ProjectStatus status;
        protected String repository;

        protected ABuilder() {

        }

        public T name(String name) {
            this.name = name;
            return self();
        }

        public T branch(String branch) {
            this.branch = branch;
            return self();
        }

        public T id(ProjectIdModel id) {
            this.id = id;
            return self();
        }

        public T status(ProjectStatus status) {
            this.status = status;
            return self();
        }

        public T repository(String repository) {
            this.repository = repository;
            return self();
        }

        protected abstract T self();
    }
}
