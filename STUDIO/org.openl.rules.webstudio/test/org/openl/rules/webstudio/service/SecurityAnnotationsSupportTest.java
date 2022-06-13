package org.openl.rules.webstudio.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.domain.BasePermission;
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
public class SecurityAnnotationsSupportTest {
    @Autowired
    MutableAclService aclService;

    @Autowired
    SecuredService securedService;

    @Test(expected = AccessDeniedException.class)
    @WithMockUser(value = "oleg")
    public void hasRoleAnnotationsDenyTest() {
        Assert.assertNotNull(securedService);
        securedService.save(new Foo(44L));
    }

    @Test
    @WithMockUser(value = "oleg", roles = "Developers")
    public void hasRoleAnnotationsAllowTest() {
        Assert.assertNotNull(securedService);
        securedService.save(new Foo(44L));
    }

    @Test
    @WithMockUser(value = "oleg")
    @Transactional
    public void hasPermissionAnnotationsTest() {
        Assert.assertNotNull(securedService);
        Foo foo = new Foo(45L);
        // Prepare the information we'd like in our access control entry (ACE)
        ObjectIdentity oi = new ObjectIdentityImpl(foo);

        // Create or update the relevant ACL
        MutableAcl acl;
        try {
            acl = (MutableAcl) aclService.readAclById(oi);
        } catch (NotFoundException nfe) {
            acl = aclService.createAcl(oi);
        }

        // Now grant some permissions via an access control entry (ACE)
        Sid sid = new PrincipalSid("oleg");
        acl.insertAce(acl.getEntries().size(), BasePermission.READ, sid, false);
        aclService.updateAcl(acl);
        try {
            securedService.read(foo);
            Assert.fail("Expected access deny exception");
        } catch (AccessDeniedException ignored) {
        }
        acl.deleteAce(acl.getEntries().size() - 1);
        acl.insertAce(acl.getEntries().size(), BasePermission.READ, sid, true);
        aclService.updateAcl(acl);
        securedService.read(foo);
    }
}
