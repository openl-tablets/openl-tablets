package org.openl.studio.projects.model;

public class ProjectDependencyViewModel extends AProjectViewModel {

    private ProjectDependencyViewModel(Builder from) {
        super(from);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ABuilder<Builder> {

        private Builder() {
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
