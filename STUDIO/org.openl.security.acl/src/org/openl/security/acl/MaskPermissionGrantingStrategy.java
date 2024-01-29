package org.openl.security.acl;

import org.springframework.security.acls.domain.AuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Permission;

import org.openl.security.acl.permission.AclPermission;

public class MaskPermissionGrantingStrategy extends DefaultPermissionGrantingStrategy {
    public MaskPermissionGrantingStrategy(AuditLogger auditLogger) {
        super(auditLogger);
    }

    @Override
    protected boolean isGranted(AccessControlEntry ace, Permission p) {
        if (p.getMask() >= 1 << AclPermission.MASK_END) {
            return ace.getPermission().getMask() == p.getMask();
        }
        if (ace.isGranting() && p.getMask() != 0) {
            return (ace.getPermission().getMask() & p.getMask()) == p.getMask();
        } else {
            return ace.getPermission().getMask() == p.getMask();
        }
    }
}
