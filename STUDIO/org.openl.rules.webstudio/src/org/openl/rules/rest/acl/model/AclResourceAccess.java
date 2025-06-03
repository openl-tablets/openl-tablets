package org.openl.rules.rest.acl.model;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Access control entries for a specific resource")
@JsonDeserialize(builder = AclResourceAccess.Builder.class)
public class AclResourceAccess {

    @Parameter(description = "Resource reference (e.g., repo or project)")
    @NotNull
    @Valid
    private final AclResourceRef resourceRef;

    @Parameter(description = "List of access control entries for this resource")
    @NotNull
    @Size(min = 1)
    @Valid
    private final List<AccessControlEntry> aces;

    private AclResourceAccess(Builder builder) {
        this.resourceRef = builder.resourceRef;
        this.aces = Optional.ofNullable(builder.aces)
                .map(List::copyOf)
                .orElse(null);
    }

    public AclResourceRef getResourceRef() {
        return resourceRef;
    }

    public List<AccessControlEntry> getAces() {
        return aces;
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private AclResourceRef resourceRef;
        private List<AccessControlEntry> aces;

        private Builder() {
        }

        public Builder resourceRef(AclResourceRef resourceRef) {
            this.resourceRef = resourceRef;
            return this;
        }

        public Builder aces(List<AccessControlEntry> aces) {
            this.aces = aces;
            return this;
        }

        public AclResourceAccess build() {
            return new AclResourceAccess(this);
        }
    }
}
