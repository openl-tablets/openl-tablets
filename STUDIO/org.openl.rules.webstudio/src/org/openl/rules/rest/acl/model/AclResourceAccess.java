package org.openl.rules.rest.acl.model;

import java.util.List;
import java.util.Optional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;


@Schema(description = "Access control entries for a specific resource")
@JsonDeserialize(builder = AclResourceAccess.Builder.class)
public class AclResourceAccess {

    @Getter
    @Parameter(description = "Resource reference (e.g., repo or project)")
    @NotNull
    @Valid
    private final AclResourceRef resourceRef;

    @Getter
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
