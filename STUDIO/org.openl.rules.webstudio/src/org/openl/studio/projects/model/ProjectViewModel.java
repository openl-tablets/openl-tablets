package org.openl.studio.projects.model;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;

import org.openl.studio.common.model.GenericView;
import org.openl.studio.projects.model.project.status.ProjectStatusViewModel;
import org.openl.util.StringUtils;

public class ProjectViewModel extends AProjectViewModel {

    @Parameter(description = "Author of latest update", required = true)
    @JsonView(GenericView.Full.class)
    public final String modifiedBy;

    @Parameter(description = "Date and time of latest update", required = true)
    @JsonView(GenericView.Full.class)
    public final ZonedDateTime modifiedAt;

    @Parameter(description = "Lock info")
    @JsonView(GenericView.Full.class)
    public final ProjectLockInfo lockInfo;

    @Parameter(description = "Revision ID", required = true)
    @JsonView({GenericView.CreateOrUpdate.class, GenericView.Full.class})
    public final String revision;

    @Parameter(description = "Project path in target repository. Can be absent if Design Repository is flat")
    @JsonView(GenericView.Full.class)
    public final String path;

    @Parameter(description = "Project Tags")
    @JsonView(GenericView.Full.class)
    public final Map<String, String> tags;

    @Parameter(description = "Project Comment")
    @JsonView(GenericView.Full.class)
    public final String comment;

    @Parameter(description = "Project Dependencies")
    @JsonView(GenericView.Full.class)
    public final List<ProjectDependencyViewModel> dependencies;

    @Parameter(description = "Projects that depend on this one. Present only on the single-project response")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public final List<ProjectDependencyViewModel> usedBy;

    @Parameter(description = "The project as its rules.xml describes it. Present only when the descriptor is requested.")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public final DescriptorViewModel descriptor;

    @Parameter(description = "Whether the project's current branch is protected")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public final boolean branchProtected;

    @Parameter(description = "Whether the project's current branch is the repository main branch")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public final boolean branchDefault;

    @Parameter(description = "The repository the project is stored in. Travels with the project, so it is readable without access to the repository as a whole")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public final ProjectRepositoryModel repositoryInfo;

    @Parameter(description = "Capabilities of the current user on the project")
    @JsonView(GenericView.Full.class)
    public final ProjectCapabilities capabilities;

    @Parameter(description = "Current compilation status. Present only when requested.")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public final ProjectStatusViewModel compileStatus;

    private ProjectViewModel(Builder from) {
        super(from);
        this.modifiedBy = from.modifiedBy;
        this.modifiedAt = from.modifiedAt;
        this.revision = from.revision;
        this.path = from.path;
        this.lockInfo = from.lockInfo;
        this.tags = new TreeMap<>(from.tags);
        this.comment = from.comment;
        this.dependencies = Optional.ofNullable(from.dependencies).map(List::copyOf).orElseGet(List::of);
        this.usedBy = Optional.ofNullable(from.usedBy).map(List::copyOf).orElseGet(List::of);
        this.descriptor = from.descriptor;
        this.branchProtected = from.branchProtected;
        this.branchDefault = from.branchDefault;
        this.repositoryInfo = from.repositoryInfo;
        this.capabilities = from.capabilities;
        this.compileStatus = from.compileStatus;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ABuilder<Builder> {
        private String modifiedBy;
        private ZonedDateTime modifiedAt;
        private ProjectLockInfo lockInfo;
        private String revision;
        private String path;
        private final Map<String, String> tags = new HashMap<>();
        private String comment;
        private List<ProjectDependencyViewModel> dependencies;
        private List<ProjectDependencyViewModel> usedBy;
        private DescriptorViewModel descriptor;
        private boolean branchProtected;
        private boolean branchDefault;
        private ProjectRepositoryModel repositoryInfo;
        private ProjectCapabilities capabilities;
        private ProjectStatusViewModel compileStatus;

        private Builder() {
        }

        public Builder modifiedBy(String modifiedBy) {
            this.modifiedBy = modifiedBy;
            return this;
        }

        public Builder lockInfo(ProjectLockInfo lockInfo) {
            this.lockInfo = lockInfo;
            return this;
        }

        public Builder modifiedAt(ZonedDateTime modifiedAt) {
            this.modifiedAt = modifiedAt;
            return this;
        }

        public Builder revision(String revision) {
            this.revision = revision;
            return this;
        }

        public Builder path(String path) {
            this.path = StringUtils.isEmpty(path) ? null : path;
            return this;
        }

        public Builder addTag(String name, String value) {
            this.tags.put(name, value);
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder addDependency(ProjectDependencyViewModel dependency) {
            if (dependencies == null) {
                dependencies = new ArrayList<>();
            }
            this.dependencies.add(dependency);
            return this;
        }

        public Builder addUsedBy(ProjectDependencyViewModel usedBy) {
            if (this.usedBy == null) {
                this.usedBy = new ArrayList<>();
            }
            this.usedBy.add(usedBy);
            return this;
        }

        public Builder descriptor(DescriptorViewModel descriptor) {
            this.descriptor = descriptor;
            return this;
        }

        public Builder branchProtected(boolean branchProtected) {
            this.branchProtected = branchProtected;
            return this;
        }

        public Builder branchDefault(boolean branchDefault) {
            this.branchDefault = branchDefault;
            return this;
        }

        public Builder repositoryInfo(ProjectRepositoryModel repositoryInfo) {
            this.repositoryInfo = repositoryInfo;
            return this;
        }

        public Builder capabilities(ProjectCapabilities capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        public Builder compileStatus(ProjectStatusViewModel compileStatus) {
            this.compileStatus = compileStatus;
            return this;
        }

        @Override
        protected Builder self() {
            return this;
        }

        public ProjectViewModel build() {
            return new ProjectViewModel(this);
        }
    }
}
