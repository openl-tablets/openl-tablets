package org.openl.rules.webstudio.service;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { DBTestConfiguration.class, AclServiceTestConfiguration.class })
@TestPropertySource(properties = { "db.url = jdbc:h2:mem:temp;MODE=LEGACY;DB_CLOSE_DELAY=-1",
        "db.user =",
        "db.password =" })
public class AclServiceTest {

    @Autowired
    MutableAclService aclService;

    @Test
    public void exists() {
        Assert.assertNotNull(aclService);
    }

    @Test
    @WithMockUser(value = "admin")
    @Transactional
    public void grantAndReadPermission() {
        // Prepare the information we'd like in our access control entry (ACE)
        ObjectIdentity oi = new ObjectIdentityImpl(Foo.class, 44L);

        // Create or update the relevant ACL
        MutableAcl acl;
        try {
            acl = (MutableAcl) aclService.readAclById(oi);
        } catch (NotFoundException nfe) {
            acl = aclService.createAcl(oi);
        }

        // Now grant some permissions via an access control entry (ACE)
        Sid sid = new PrincipalSid("Samantha");
        acl.insertAce(acl.getEntries().size(), BasePermission.WRITE, sid, true);
        acl.insertAce(acl.getEntries().size(), BasePermission.READ, sid, true);
        acl.insertAce(acl.getEntries().size(), BasePermission.DELETE, sid, false);

        // Authority Permissions
        Sid sid1 = new GrantedAuthoritySid("Samantha");
        acl.insertAce(acl.getEntries().size(), BasePermission.WRITE, sid1, false);
        aclService.updateAcl(acl);

        try {
            acl = (MutableAcl) aclService.readAclById(oi);
        } catch (NotFoundException nfe) {
            Assert.fail("ACL is not found!");
        }

        Assert.assertTrue(acl.isGranted(List.of(BasePermission.WRITE), List.of(sid), false));
        Assert.assertTrue(acl.isGranted(List.of(BasePermission.READ), List.of(sid), false));
        Assert.assertFalse(acl.isGranted(List.of(BasePermission.DELETE), List.of(sid), false));
        // Authority Permissions
        Assert.assertFalse(acl.isGranted(List.of(BasePermission.WRITE), List.of(sid1), false));
    }
}
