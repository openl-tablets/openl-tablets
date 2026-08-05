package org.openl.studio.projects.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;

import org.openl.studio.common.model.GenericView;

public class ProjectDependencyViewModel extends AProjectViewModel {

    @Parameter(description = "Whether the dependency's branch is protected")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public final boolean branchProtected;

    @Parameter(description = "Whether the dependency's branch is the repository main branch")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public final boolean branchDefault;

    @Parameter(description = "Whether the declared project is absent from the workspace. Such a dependency carries its name alone")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public final boolean missing;

    @Parameter(description = "Whether another dependency declares this one instead of the project itself")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public final boolean transitive;

    private ProjectDependencyViewModel(Builder from) {
        super(from);
        this.branchProtected = from.branchProtected;
        this.branchDefault = from.branchDefault;
        this.missing = from.missing;
        this.transitive = from.transitive;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ABuilder<Builder> {

        private boolean branchProtected;
        private boolean branchDefault;
        private boolean missing;
        private boolean transitive;

        private Builder() {
        }

        public Builder branchProtected(boolean branchProtected) {
            this.branchProtected = branchProtected;
            return this;
        }

        public Builder branchDefault(boolean branchDefault) {
            this.branchDefault = branchDefault;
            return this;
        }

        public Builder missing(boolean missing) {
            this.missing = missing;
            return this;
        }

        public Builder transitive(boolean transitive) {
            this.transitive = transitive;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        public ProjectDependencyViewModel build() {
            return new ProjectDependencyViewModel(this);
        }
    }
}
