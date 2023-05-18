package org.openl.security.acl;

import java.util.List;

import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

// This code is related a bug in Spring Security https://github.com/spring-projects/spring-security/issues/4186
// After fixing this bug, this code should be removed.
public class AclAuthorizationStrategyImpl extends org.springframework.security.acls.domain.AclAuthorizationStrategyImpl {

    private SidRetrievalStrategy sidRetrievalStrategy;

    private final GrantedAuthority gaGeneralChanges;

    private final GrantedAuthority gaModifyAuditing;

    private final GrantedAuthority gaTakeOwnership;

    public AclAuthorizationStrategyImpl(GrantedAuthority... auths) {
        super(auths);
        if (auths.length == 3) {
            this.gaTakeOwnership = auths[0];
            this.gaModifyAuditing = auths[1];
            this.gaGeneralChanges = auths[2];
        } else {
            this.gaTakeOwnership = auths[0];
            this.gaModifyAuditing = auths[0];
            this.gaGeneralChanges = auths[0];
        }
    }

    @Override
    public void securityCheck(Acl acl, int changeType) {
        List<Sid> sids = sidRetrievalStrategy.getSids(SecurityContextHolder.getContext().getAuthentication());
        for (Sid sid : sids) {
            if (sid instanceof GrantedAuthoritySid) {
                GrantedAuthoritySid grantedAuthoritySid = (GrantedAuthoritySid) sid;
                if (getRequiredAuthority(changeType).getAuthority().equals(grantedAuthoritySid.getGrantedAuthority())) {
                    return;
                }
            }
        }
        super.securityCheck(acl, changeType);
    }

    private GrantedAuthority getRequiredAuthority(int changeType) {
        if (changeType == CHANGE_AUDITING) {
            return this.gaModifyAuditing;
        }
        if (changeType == CHANGE_GENERAL) {
            return this.gaGeneralChanges;
        }
        if (changeType == CHANGE_OWNERSHIP) {
            return this.gaTakeOwnership;
        }
        throw new IllegalArgumentException("Unknown change type");
    }

    @Override
    public void setSidRetrievalStrategy(SidRetrievalStrategy sidRetrievalStrategy) {
        super.setSidRetrievalStrategy(sidRetrievalStrategy);
        this.sidRetrievalStrategy = sidRetrievalStrategy;
    }
}
