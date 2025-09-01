package org.openl.rules.rest.deployment.model;

import java.time.ZonedDateTime;

public class DeploymentItemViewModel {

    public final String name;

    public final String modifiedBy;

    public final ZonedDateTime modifiedAt;

    public final String revision;

    public DeploymentItemViewModel(Builder from) {
        this.name = from.name;
        this.modifiedBy = from.modifiedBy;
        this.modifiedAt = from.modifiedAt;
        this.revision = from.revision;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String modifiedBy;
        private ZonedDateTime modifiedAt;
        private String revision;

        public Builder name(String name) {
            this.name = name;
            return this;
        }
        public Builder modifiedBy(String modifiedBy) {
            this.modifiedBy = modifiedBy;
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

        public DeploymentItemViewModel build() {
            return new DeploymentItemViewModel(this);
        }
    }

}
