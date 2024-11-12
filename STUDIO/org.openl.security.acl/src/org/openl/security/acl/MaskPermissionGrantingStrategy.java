package org.openl.security.acl;

import org.springframework.security.acls.domain.AuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Permission;

public class MaskPermissionGrantingStrategy extends DefaultPermissionGrantingStrategy {
    public MaskPermissionGrantingStrategy(AuditLogger auditLogger) {
        super(auditLogger);
    }

    @Override
    protected boolean isGranted(AccessControlEntry ace, Permission p) {
        var acep = ace.getPermission();
        if (ace.isGranting() && p.getMask() != 0) {
            return (acep.getMask() & p.getMask()) == p.getMask();
        } else {
            return acep.getMask() == p.getMask();
        }
    }
}
