package org.openl.rules.rest.acl.model;

import java.util.Comparator;
import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;

@JsonDeserialize(builder = AclSubject.Builder.class)
public class AclSubject {

    public static final Comparator<AclSubject> COMPARATOR = Comparator.comparing(AclSubject::getSid);

    @Parameter(description = "SID name")
    @NotEmpty
    private final String sid;

    @Parameter(description = "Is principal")
    private final Boolean principal;

    private AclSubject(Builder builder) {
        this.sid = builder.sid;
        this.principal = builder.principal;
    }

    public String getSid() {
        return sid;
    }

    public Boolean getPrincipal() {
        return principal;
    }

    public static AclSubject of(Sid sid) {
        var builder = builder();
        switch (sid) {
            case PrincipalSid user -> builder.principal(true).sid(user.getPrincipal());
            case GrantedAuthoritySid group -> builder.sid(group.getGrantedAuthority());
            default -> throw new IllegalArgumentException("Unsupported Sid type: " + sid.getClass());
        }
        return builder.build();
    }

    public Sid toSid() {
        if (Boolean.TRUE.equals(principal)) {
            return new PrincipalSid(sid);
        } else {
            return new GrantedAuthoritySid(sid);
        }
    }

    @JsonCreator
    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private String sid;
        private Boolean principal;

        private Builder() {
        }

        public Builder sid(String sid) {
            this.sid = sid;
            return this;
        }

        public Builder principal(Boolean principal) {
            this.principal = principal;
            return this;
        }

        public AclSubject build() {
            return new AclSubject(this);
        }
    }

}
