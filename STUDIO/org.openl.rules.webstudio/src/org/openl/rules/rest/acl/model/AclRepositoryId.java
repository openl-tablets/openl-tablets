package org.openl.rules.rest.acl.model;

import java.util.Base64;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

import org.openl.security.acl.repository.AclRepositoryType;

@Schema(implementation = String.class)
public class AclRepositoryId {

    private static final String ID_SEPARATOR = ":";

    @NotNull
    private final AclRepositoryType type;

    private final String id;

    private AclRepositoryId(Builder builder) {
        this.type = builder.type;
        this.id = builder.id;
    }

    public AclRepositoryType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    @JsonValue
    public String encode() {
        String src = type.name();
        if (id != null) {
            src += ID_SEPARATOR + id;
        }
        return Base64.getEncoder().encodeToString(src.getBytes());
    }

    @JsonCreator
    public static AclRepositoryId decode(String encoded) {
        String decoded = new String(Base64.getDecoder().decode(encoded));
        String[] parts = decoded.split(":");
        if (parts.length > 2) {
            throw new IllegalArgumentException("Invalid id value: " + encoded);
        }
        var builder = builder()
            .type(AclRepositoryType.valueOf(parts[0]));
        if (parts.length > 1) {
            builder.id(parts[1]);
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AclRepositoryType type;
        private String id;

        private Builder() {
        }

        public Builder type(AclRepositoryType type) {
            this.type = type;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public AclRepositoryId build() {
            return new AclRepositoryId(this);
        }
    }

}
