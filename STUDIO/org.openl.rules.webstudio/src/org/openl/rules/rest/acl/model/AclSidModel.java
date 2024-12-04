package org.openl.rules.rest.acl.model;

import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;

public class AclSidModel {

    private final String sid;
    private final Boolean principal;


    public AclSidModel(Builder builder) {
        this.sid = builder.sid;
        this.principal = builder.principal;
    }

    public String getSid() {
        return sid;
    }

    public Boolean getPrincipal() {
        return principal;
    }

    public static AclSidModel of(Sid sid) {
        var builder = builder();
        if (sid instanceof PrincipalSid) {
            builder.principal(true)
                    .sid(((PrincipalSid) sid).getPrincipal());
        } else if (sid instanceof GrantedAuthoritySid) {
            builder.sid(((GrantedAuthoritySid) sid).getGrantedAuthority());
        } else {
            throw new IllegalArgumentException("Unsupported Sid type: " + sid.getClass());
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

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

        public AclSidModel build() {
            return new AclSidModel(this);
        }
    }

}
