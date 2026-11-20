package org.openl.studio.projects.model;

import java.util.List;

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

    @Parameter(description = """
            Maven coordinates of the project's archive the dependency names. Absent when it names none""")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public final String mavenArtifact;

    @Parameter(description = """
            Whether the project compiles with the archive downloaded from the Maven repository, since the user has no \
            project of that name open""")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public final boolean maven;

    @Parameter(description = """
            Other dependencies of the name, which ask for other Maven coordinates and are left out: one project of a \
            name is compiled, the one reached first. Absent when there is none""")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public final List<ProjectDependencyConflictView> conflicts;

    private ProjectDependencyViewModel(Builder from) {
        super(from);
        this.branchProtected = from.branchProtected;
        this.branchDefault = from.branchDefault;
        this.missing = from.missing;
        this.transitive = from.transitive;
        this.mavenArtifact = from.mavenArtifact;
        this.maven = from.maven;
        this.conflicts = from.conflicts;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ABuilder<Builder> {

        private boolean branchProtected;
        private boolean branchDefault;
        private boolean missing;
        private boolean transitive;
        private String mavenArtifact;
        private List<ProjectDependencyConflictView> conflicts = List.of();
        private boolean maven;

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

        public Builder mavenArtifact(String mavenArtifact) {
            this.mavenArtifact = mavenArtifact;
            return this;
        }

        public Builder conflicts(List<ProjectDependencyConflictView> conflicts) {
            this.conflicts = List.copyOf(conflicts);
            return this;
        }

        public Builder maven(boolean maven) {
            this.maven = maven;
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
