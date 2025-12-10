package org.openl.studio.projects.model;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;

import org.openl.studio.common.model.GenericView;
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

        @Override
        protected Builder self() {
            return this;
        }

        public ProjectViewModel build() {
            return new ProjectViewModel(this);
        }
    }
}
