package org.openl.rules.webstudio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.security.acl.permission.AclPermission.DESIGN_REPOSITORY_WRITE;
import static org.openl.security.acl.permission.AclPermission.EDIT;
import static org.openl.security.acl.permission.AclPermission.VIEW;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.SimpleUser;
import org.openl.security.acl.repository.RepositoryAclService;

@SpringJUnitConfig(classes = {DBTestConfiguration.class, AclServiceTestConfiguration.class})
@TestPropertySource(properties = {"db.url = jdbc:h2:mem:temp;DB_CLOSE_DELAY=-1",
        "db.user =",
        "db.password ="})
@WithMockUser(value = "admin", authorities = "ADMIN")
public class RepositoryAclServiceTest {

    private static final String DEVELOPERS_JUNIT = "DEVELOPERS_JUNIT";

    @Autowired
    @Qualifier("designRepositoryAclService")
    RepositoryAclService designRepositoryAclService;

    @Autowired
    SpringCacheBasedAclCache springCacheBasedAclCache;

    @BeforeEach
    public void before() {
        springCacheBasedAclCache.clearCache();
    }

    @Test
    public void exists() {
        assertNotNull(designRepositoryAclService);
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
        assertTrue(
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
        assertTrue(designRepositoryAclService
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
        assertTrue(
                designRepositoryAclService.isGranted("repoId2", "/projectName1/rules/module1.xlsx", List.of(EDIT)));
    }

    @Test
    @WithMockUser(value = "admin", authorities = "ADMIN")
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
        assertEquals(1,
                designRepositoryAclService.listPermissions("repoId2", "/projectName1/rules")
                        .get(new GrantedAuthoritySid(DEVELOPERS_JUNIT))
                        .size());
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    public void permissionRemovingByPermission() {
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(EDIT)));

        Authentication mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.addPermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(EDIT, VIEW),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(EDIT)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(EDIT),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(EDIT)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(VIEW),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(EDIT)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    public void permissionRemovingBySids() {
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
        Authentication mockUser = setAndreyAuthenticationToContext();
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));

        setAdminAuthenticationToContext();
        designRepositoryAclService.addPermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(VIEW),
                List.of(new PrincipalSid("oleg"), new PrincipalSid("andrey")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));

        setAndreyAuthenticationToContext();
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));

        setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(VIEW),
                List.of(new PrincipalSid("andrey")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));

        setAndreyAuthenticationToContext();
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(VIEW),
                List.of(new PrincipalSid("oleg")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
        setAndreyAuthenticationToContext();
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Rollback
    @Transactional
    public void delete() {
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(VIEW)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));

        Authentication mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService
                .addPermissions("repoId1", "/projectName1/rules", List.of(VIEW), List.of(new PrincipalSid("oleg")));
        designRepositoryAclService.addPermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(VIEW),
                List.of(new PrincipalSid("oleg")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(VIEW)));
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.deleteAcl("repoId1", "/projectName1/rules");

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(VIEW)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Rollback
    @Transactional
    public void move() {
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(VIEW)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(EDIT)));

        Authentication mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService
                .addPermissions("repoId1", "/projectName1/rules", List.of(VIEW), List.of(new PrincipalSid("oleg")));
        designRepositoryAclService.addPermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(VIEW, EDIT),
                List.of(new PrincipalSid("oleg")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(VIEW)));
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(VIEW)));
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(EDIT)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.move("repoId1", "/projectName1/rules", "/projectName1/rules1/rules2");

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules1/rules2", List.of(VIEW)));
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules1/rules2/module1.xlsx", List.of(VIEW)));
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules1/rules2/module1.xlsx", List.of(EDIT)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    public void nullPathSupport() {
        assertFalse(designRepositoryAclService.isGranted("repoId1", null, List.of(VIEW)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "", List.of(VIEW)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/", List.of(VIEW)));

        Authentication mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService
                .addPermissions("repoId1", null, List.of(VIEW), List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService.isGranted("repoId1", null, List.of(VIEW)));
        assertTrue(designRepositoryAclService.isGranted("repoId1", "", List.of(VIEW)));
        assertTrue(designRepositoryAclService.isGranted("repoId1", "/", List.of(VIEW)));

        setAdminAuthenticationToContext();
        designRepositoryAclService
                .removePermissions("repoId1", "", List.of(VIEW), List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertFalse(designRepositoryAclService.isGranted("repoId1", null, List.of(VIEW)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "", List.of(VIEW)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/", List.of(VIEW)));

        setAdminAuthenticationToContext();
        designRepositoryAclService
                .addPermissions("repoId1", "", List.of(VIEW), List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService.isGranted("repoId1", null, List.of(VIEW)));
        assertTrue(designRepositoryAclService.isGranted("repoId1", "", List.of(VIEW)));
        assertTrue(designRepositoryAclService.isGranted("repoId1", "/", List.of(VIEW)));

        setAdminAuthenticationToContext();
        designRepositoryAclService
                .removePermissions("repoId1", "/", List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertFalse(designRepositoryAclService.isGranted("repoId1", null, List.of(VIEW)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "", List.of(VIEW)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/", List.of(VIEW)));

        setAdminAuthenticationToContext();
        designRepositoryAclService
                .addPermissions("repoId1", "/", List.of(VIEW), List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService.isGranted("repoId1", null, List.of(VIEW)));
        assertTrue(designRepositoryAclService.isGranted("repoId1", "", List.of(VIEW)));
        assertTrue(designRepositoryAclService.isGranted("repoId1", "/", List.of(VIEW)));

        setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1", null);

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertFalse(designRepositoryAclService.isGranted("repoId1", null, List.of(VIEW)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "", List.of(VIEW)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/", List.of(VIEW)));

    }

    private Authentication setAdminAuthenticationToContext() {
        Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();
        SimpleUser principal = SimpleUser.builder()
                .setUsername("admin")
                .setPrivileges(List.of(Privileges.ADMIN))
                .build();
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
