package org.openl.rules.webstudio.service;

import static org.openl.security.acl.permission.AclPermission.DESIGN_REPOSITORY_WRITE;
import static org.openl.security.acl.permission.AclPermission.EDIT;
import static org.openl.security.acl.permission.AclPermission.VIEW;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.SimpleUser;
import org.openl.security.acl.repository.DesignRepositoryAclService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { DBTestConfiguration.class, AclServiceTestConfiguration.class })
@TestPropertySource(properties = { "db.url = jdbc:h2:mem:temp;MODE=LEGACY;DB_CLOSE_DELAY=-1",
        "db.user =",
        "db.password =" })
@WithMockUser(value = "admin", authorities = "Administrators")
public class DesignRepositoryAclServiceTest {

    private static final String DEVELOPERS_JUNIT = "DEVELOPERS_JUNIT";

    @Autowired
    DesignRepositoryAclService designRepositoryAclService;

    @Autowired
    SpringCacheBasedAclCache springCacheBasedAclCache;

    @Before
    public void before() {
        springCacheBasedAclCache.clearCache();
    }

    @Test
    public void exists() {
        Assert.assertNotNull(designRepositoryAclService);
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    public void permissionChecking() {
        Authentication mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.addPermissions("repoId1",
            "/projectName1/rules/module1.xlsx",
            List.of(EDIT),
            List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert.assertTrue(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(EDIT)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    public void permissionMaskChecking() {
        Authentication mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.addPermissions("repoId2",
            "/projectName1/rules",
            List.of(EDIT),
            List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert.assertTrue(designRepositoryAclService
            .isGranted("repoId2", "/projectName1/rules/module1.xlsx", List.of(DESIGN_REPOSITORY_WRITE)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    public void permissionInheritanceChecking() {
        Authentication mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.addPermissions("repoId2",
            "/projectName1/rules",
            List.of(EDIT),
            List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert.assertTrue(
            designRepositoryAclService.isGranted("repoId2", "/projectName1/rules/module1.xlsx", List.of(EDIT)));
    }

    @Test
    @WithMockUser(value = "admin", authorities = "Administrators")
    @Transactional
    @Rollback
    public void permissionDuplicateChecking() {
        designRepositoryAclService.addPermissions("repoId2",
            "/projectName1/rules",
            List.of(EDIT),
            List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));
        designRepositoryAclService.addPermissions("repoId2",
            "/projectName1/rules",
            List.of(EDIT),
            List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));
        designRepositoryAclService.addPermissions("repoId2",
            "/projectName1/rules",
            List.of(EDIT),
            List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));
        Assert.assertEquals(1,
            designRepositoryAclService.listPermissions("repoId2", "/projectName1/rules")
                .get(new GrantedAuthoritySid(DEVELOPERS_JUNIT))
                .size());
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    public void permissionRemovingByPermission() {
        Assert.assertFalse(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
        Assert.assertFalse(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(EDIT)));

        Authentication mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.addPermissions("repoId1",
            "/projectName1/rules/module1.xlsx",
            List.of(EDIT, VIEW),
            List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert.assertTrue(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
        Assert.assertTrue(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(EDIT)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1",
            "/projectName1/rules/module1.xlsx",
            List.of(EDIT),
            List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert.assertTrue(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
        Assert.assertFalse(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(EDIT)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1",
            "/projectName1/rules/module1.xlsx",
            List.of(VIEW),
            List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert.assertFalse(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
        Assert.assertFalse(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(EDIT)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    public void permissionRemovingBySids() {
        Assert.assertFalse(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
        Authentication mockUser = setAndreyAuthenticationToContext();
        Assert.assertFalse(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));

        setAdminAuthenticationToContext();
        designRepositoryAclService.addPermissions("repoId1",
            "/projectName1/rules/module1.xlsx",
            List.of(VIEW),
            List.of(new PrincipalSid("oleg"), new PrincipalSid("andrey")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert.assertTrue(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));

        setAndreyAuthenticationToContext();
        Assert.assertTrue(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));

        setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1",
            "/projectName1/rules/module1.xlsx",
            List.of(VIEW),
            List.of(new PrincipalSid("andrey")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert.assertTrue(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));

        setAndreyAuthenticationToContext();
        Assert.assertFalse(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1",
            "/projectName1/rules/module1.xlsx",
            List.of(VIEW),
            List.of(new PrincipalSid("oleg")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert.assertFalse(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
        setAndreyAuthenticationToContext();
        Assert.assertFalse(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Rollback
    @Transactional
    public void delete() {
        Assert.assertFalse(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(VIEW)));
        Assert.assertFalse(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));

        Authentication mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService
            .addPermissions("repoId1", "/projectName1/rules", List.of(VIEW), List.of(new PrincipalSid("oleg")));
        designRepositoryAclService.addPermissions("repoId1",
            "/projectName1/rules/module1.xlsx",
            List.of(VIEW),
            List.of(new PrincipalSid("oleg")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert.assertTrue(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(VIEW)));
        Assert.assertTrue(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.delete("repoId1", "/projectName1/rules");

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert.assertFalse(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(VIEW)));
        Assert.assertFalse(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Rollback
    @Transactional
    public void move() {
        Assert.assertFalse(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(VIEW)));
        Assert.assertFalse(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
        Assert.assertFalse(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(EDIT)));

        Authentication mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService
            .addPermissions("repoId1", "/projectName1/rules", List.of(VIEW), List.of(new PrincipalSid("oleg")));
        designRepositoryAclService.addPermissions("repoId1",
            "/projectName1/rules/module1.xlsx",
            List.of(VIEW, EDIT),
            List.of(new PrincipalSid("oleg")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert.assertTrue(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(VIEW)));
        Assert.assertTrue(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
        Assert.assertTrue(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(EDIT)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.move("repoId1", "/projectName1/rules", "/projectName1/rules1/rules2");

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert
            .assertTrue(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules1/rules2", List.of(VIEW)));
        Assert.assertTrue(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules1/rules2/module1.xlsx", List.of(VIEW)));
        Assert.assertTrue(
            designRepositoryAclService.isGranted("repoId1", "/projectName1/rules1/rules2/module1.xlsx", List.of(EDIT)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    public void nullPathSupport() {
        Assert.assertFalse(designRepositoryAclService.isGranted("repoId1", null, List.of(VIEW)));
        Assert.assertFalse(designRepositoryAclService.isGranted("repoId1", "", List.of(VIEW)));
        Assert.assertFalse(designRepositoryAclService.isGranted("repoId1", "/", List.of(VIEW)));

        Authentication mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService
            .addPermissions("repoId1", null, List.of(VIEW), List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert.assertTrue(designRepositoryAclService.isGranted("repoId1", null, List.of(VIEW)));
        Assert.assertTrue(designRepositoryAclService.isGranted("repoId1", "", List.of(VIEW)));
        Assert.assertTrue(designRepositoryAclService.isGranted("repoId1", "/", List.of(VIEW)));

        setAdminAuthenticationToContext();
        designRepositoryAclService
            .removePermissions("repoId1", "", List.of(VIEW), List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert.assertFalse(designRepositoryAclService.isGranted("repoId1", null, List.of(VIEW)));
        Assert.assertFalse(designRepositoryAclService.isGranted("repoId1", "", List.of(VIEW)));
        Assert.assertFalse(designRepositoryAclService.isGranted("repoId1", "/", List.of(VIEW)));

        setAdminAuthenticationToContext();
        designRepositoryAclService
            .addPermissions("repoId1", "", List.of(VIEW), List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert.assertTrue(designRepositoryAclService.isGranted("repoId1", null, List.of(VIEW)));
        Assert.assertTrue(designRepositoryAclService.isGranted("repoId1", "", List.of(VIEW)));
        Assert.assertTrue(designRepositoryAclService.isGranted("repoId1", "/", List.of(VIEW)));

        setAdminAuthenticationToContext();
        designRepositoryAclService
            .removePermissions("repoId1", "/", List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert.assertFalse(designRepositoryAclService.isGranted("repoId1", null, List.of(VIEW)));
        Assert.assertFalse(designRepositoryAclService.isGranted("repoId1", "", List.of(VIEW)));
        Assert.assertFalse(designRepositoryAclService.isGranted("repoId1", "/", List.of(VIEW)));

        setAdminAuthenticationToContext();
        designRepositoryAclService
            .addPermissions("repoId1", "/", List.of(VIEW), List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert.assertTrue(designRepositoryAclService.isGranted("repoId1", null, List.of(VIEW)));
        Assert.assertTrue(designRepositoryAclService.isGranted("repoId1", "", List.of(VIEW)));
        Assert.assertTrue(designRepositoryAclService.isGranted("repoId1", "/", List.of(VIEW)));

        setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1", null);

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        Assert.assertFalse(designRepositoryAclService.isGranted("repoId1", null, List.of(VIEW)));
        Assert.assertFalse(designRepositoryAclService.isGranted("repoId1", "", List.of(VIEW)));
        Assert.assertFalse(designRepositoryAclService.isGranted("repoId1", "/", List.of(VIEW)));

    }

    private Authentication setAdminAuthenticationToContext() {
        Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();
        SimpleGroup group = new SimpleGroup();
        group.setName("Administrators");
        SimpleUser principal = SimpleUser.builder().setUsername("admin").setPrivileges(List.of(group)).build();
        Authentication auth = new UsernamePasswordAuthenticationToken(principal,
            "password",
            principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return currentUser;
    }

    private Authentication setAndreyAuthenticationToContext() {
        Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();
        SimpleGroup group = new SimpleGroup();
        group.setName(DEVELOPERS_JUNIT);
        SimpleUser principal = SimpleUser.builder().setUsername("andrey").setPrivileges(List.of(group)).build();
        Authentication auth = new UsernamePasswordAuthenticationToken(principal,
            "password",
            principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return currentUser;
    }
}
