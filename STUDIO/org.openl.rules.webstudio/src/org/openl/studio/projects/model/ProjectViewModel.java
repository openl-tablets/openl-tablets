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

    @Parameter(description = "The list of selected branches")
    @JsonView(GenericView.Full.class)
    public final List<String> selectedBranches;

    @Parameter(description = "Project Dependencies")
    @JsonView(GenericView.Full.class)
    public final List<ProjectDependencyViewModel> dependencies;

    @Parameter(description = "Projects that depend on this one. Present only on the single-project response")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public final List<ProjectDependencyViewModel> usedBy;

    @Parameter(description = "Project description from resolved project descriptor. Present only when modules are requested.")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public final String description;

    @Parameter(description = "Rules modules from resolved project descriptor. Present only when requested.")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public final List<ModuleViewModel> modules;

    @Parameter(description = "Properties file-name patterns from resolved project descriptor. Present only when modules are requested.")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public final List<String> versionPatterns;

    @Parameter(description = "Exposed-methods filter from resolved project descriptor. Present only when modules are requested.")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public final ExposedMethodsViewModel exposedMethods;

    @Parameter(description = "Whether the project's current branch is protected")
    @JsonView(GenericView.Full.class)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public final boolean branchProtected;

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
        this.selectedBranches = Optional.ofNullable(from.selectedBranches).map(List::copyOf).orElseGet(List::of);
        this.dependencies = Optional.ofNullable(from.dependencies).map(List::copyOf).orElseGet(List::of);
        this.usedBy = Optional.ofNullable(from.usedBy).map(List::copyOf).orElseGet(List::of);
        this.description = from.description;
        this.modules = Optional.ofNullable(from.modules).map(List::copyOf).orElseGet(List::of);
        this.versionPatterns = Optional.ofNullable(from.versionPatterns).map(List::copyOf).orElseGet(List::of);
        this.exposedMethods = from.exposedMethods;
        this.branchProtected = from.branchProtected;
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
        private List<String> selectedBranches;
        private List<ProjectDependencyViewModel> dependencies;
        private List<ProjectDependencyViewModel> usedBy;
        private String description;
        private List<ModuleViewModel> modules;
        private List<String> versionPatterns;
        private ExposedMethodsViewModel exposedMethods;
        private boolean branchProtected;
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

        public Builder selectedBranches(List<String> selectedBranches) {
            this.selectedBranches = selectedBranches;
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

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder addModule(ModuleViewModel module) {
            if (modules == null) {
                modules = new ArrayList<>();
            }
            this.modules.add(module);
            return this;
        }

        public Builder versionPatterns(List<String> versionPatterns) {
            this.versionPatterns = versionPatterns;
            return this;
        }

        public Builder exposedMethods(ExposedMethodsViewModel exposedMethods) {
            this.exposedMethods = exposedMethods;
            return this;
        }

        public Builder branchProtected(boolean branchProtected) {
            this.branchProtected = branchProtected;
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
