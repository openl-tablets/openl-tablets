package org.openl.studio.projects.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Parameter;

import org.openl.studio.common.model.GenericView;

public class ProjectViewModel extends AProjectViewModel {

    @Parameter(description = "Project Tags")
    @JsonView(GenericView.Full.class)
    public final Map<String, String> tags;

    @Parameter(description = "Project Comment")
    @JsonView(GenericView.Full.class)
    public final String comment;

    @Parameter(description = "The list of selected branches")
    @JsonView(GenericView.Full.class)
    public final List<String> selectedBranches;

    private ProjectViewModel(Builder from) {
        super(from);
        this.tags = new TreeMap<>(from.tags);
        this.comment = from.comment;
        this.selectedBranches = Optional.ofNullable(from.selectedBranches).map(List::copyOf).orElseGet(List::of);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ABuilder<Builder> {
        private final Map<String, String> tags = new HashMap<>();
        private String comment;
        private List<String> selectedBranches;

        private Builder() {
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

        @Override
        protected Builder self() {
            return this;
        }

        public ProjectViewModel build() {
            return new ProjectViewModel(this);
        }
    }
}
