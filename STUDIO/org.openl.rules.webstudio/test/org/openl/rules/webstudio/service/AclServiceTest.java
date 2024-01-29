package org.openl.rules.webstudio.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import org.openl.security.acl.permission.AclPermission;

@SpringJUnitConfig(classes = {DBTestConfiguration.class, AclServiceTestConfiguration.class})
@TestPropertySource(properties = {"db.url = jdbc:h2:mem:temp;DB_CLOSE_DELAY=-1",
  "db.user =",
  "db.password ="})
public class AclServiceTest {

    @Autowired
    MutableAclService aclService;

    @Test
    public void exists() {
        assertNotNull(aclService);
    }

    @Test
    @WithMockUser(value = "admin", authorities = "Administrators")
    @Transactional
    public void grantAndReadPermission() {
        // Prepare the information we'd like in our access control entry (ACE)
        ObjectIdentity oi = new ObjectIdentityImpl(Foo.class, "44");

        // Create or update the relevant ACL
        MutableAcl acl;
        try {
            acl = (MutableAcl) aclService.readAclById(oi);
        } catch (NotFoundException nfe) {
            acl = aclService.createAcl(oi);
        }

        // Now grant some permissions via an access control entry (ACE)
        Sid sid = new PrincipalSid("Samantha");
        acl.insertAce(acl.getEntries().size(), AclPermission.EDIT, sid, true);
        acl.insertAce(acl.getEntries().size(), AclPermission.VIEW, sid, true);
        acl.insertAce(acl.getEntries().size(), AclPermission.DELETE, sid, false);

        // Authority Permissions
        Sid sid1 = new GrantedAuthoritySid("Samantha");
        acl.insertAce(acl.getEntries().size(), AclPermission.EDIT, sid1, false);
        aclService.updateAcl(acl);

        try {
            acl = (MutableAcl) aclService.readAclById(oi);
        } catch (NotFoundException nfe) {
            fail("ACL is not found!");
        }

        assertTrue(acl.isGranted(List.of(AclPermission.EDIT), List.of(sid), false));
        assertTrue(acl.isGranted(List.of(AclPermission.VIEW), List.of(sid), false));
        assertFalse(acl.isGranted(List.of(AclPermission.DELETE), List.of(sid), false));
        // Authority Permissions
        assertFalse(acl.isGranted(List.of(AclPermission.EDIT), List.of(sid1), false));
    }
}
