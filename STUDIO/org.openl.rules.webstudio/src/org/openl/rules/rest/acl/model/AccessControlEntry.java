package org.openl.rules.rest.acl.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.security.acl.permission.AclRole;

@Schema(description = "Assigns a role to a subject for a given resource")
@JsonDeserialize(builder = AccessControlEntry.Builder.class)
public class AccessControlEntry {


    @Parameter(description = "Name of the role to assign")
    @NotNull
    private final AclRole role;

    @Parameter(description = "The subject to whom the role is assigned")
    @NotNull
    @Valid
    private final AclSubject sub;

    private AccessControlEntry(Builder builder) {
        this.role = builder.role;
        this.sub = builder.sub;
    }

    public AclRole getRole() {
        return role;
    }

    public AclSubject getSub() {
        return sub;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private AclRole role;
        private AclSubject sub;

        private Builder() {
        }

        public Builder role(AclRole role) {
            this.role = role;
            return this;
        }

        public Builder sub(AclSubject sub) {
            this.sub = sub;
            return this;
        }

        public AccessControlEntry build() {
            return new AccessControlEntry(this);
        }
    }


}
