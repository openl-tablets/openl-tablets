package org.openl.rules.webstudio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.RepositoryAclService;

@SpringJUnitConfig(classes = {DBTestConfiguration.class, AclServiceTestConfiguration.class})
@TestPropertySource(properties = {"db.url = jdbc:h2:mem:temp;DB_CLOSE_DELAY=-1",
        "db.user =",
        "db.password =",
        "db.maximumPoolSize = 3"})
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
                List.of(AclPermission.WRITE),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.WRITE)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    public void permissionMaskChecking() {
        Authentication mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.addPermissions("repoId2",
                "/projectName1/rules",
                List.of(AclPermission.WRITE),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService
                .isGranted("repoId2", "/projectName1/rules/module1.xlsx", List.of(AclPermission.WRITE)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    public void permissionInheritanceChecking() {
        Authentication mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.addPermissions("repoId2",
                "/projectName1/rules",
                List.of(AclPermission.WRITE),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(
                designRepositoryAclService.isGranted("repoId2", "/projectName1/rules/module1.xlsx", List.of(AclPermission.WRITE)));
    }

    @Test
    @WithMockUser(value = "admin", authorities = "ADMIN")
    @Transactional
    @Rollback
    public void permissionDuplicateChecking() {
        designRepositoryAclService.addPermissions("repoId2",
                "/projectName1/rules",
                List.of(AclPermission.WRITE),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));
        designRepositoryAclService.addPermissions("repoId2",
                "/projectName1/rules",
                List.of(AclPermission.WRITE),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));
        designRepositoryAclService.addPermissions("repoId2",
                "/projectName1/rules",
                List.of(AclPermission.WRITE),
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
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.READ)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.WRITE)));

        Authentication mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.addPermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(AclPermission.WRITE, AclPermission.READ),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.READ)));
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.WRITE)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(AclPermission.WRITE),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.READ)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.WRITE)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(AclPermission.READ),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.READ)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.WRITE)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    public void permissionRemovingBySids() {
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.READ)));
        Authentication mockUser = setAndreyAuthenticationToContext();
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.READ)));

        setAdminAuthenticationToContext();
        designRepositoryAclService.addPermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(AclPermission.READ),
                List.of(new PrincipalSid("oleg"), new PrincipalSid("andrey")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.READ)));

        setAndreyAuthenticationToContext();
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.READ)));

        setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(AclPermission.READ),
                List.of(new PrincipalSid("andrey")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.READ)));

        setAndreyAuthenticationToContext();
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.READ)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(AclPermission.READ),
                List.of(new PrincipalSid("oleg")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.READ)));
        setAndreyAuthenticationToContext();
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.READ)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Rollback
    @Transactional
    public void delete() {
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(AclPermission.READ)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.READ)));

        Authentication mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService
                .addPermissions("repoId1", "/projectName1/rules", List.of(AclPermission.READ), List.of(new PrincipalSid("oleg")));
        designRepositoryAclService.addPermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(AclPermission.READ),
                List.of(new PrincipalSid("oleg")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(AclPermission.READ)));
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.READ)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.deleteAcl("repoId1", "/projectName1/rules");

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(AclPermission.READ)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.READ)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Rollback
    @Transactional
    public void move() {
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(AclPermission.READ)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.READ)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.WRITE)));

        Authentication mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService
                .addPermissions("repoId1", "/projectName1/rules", List.of(AclPermission.READ), List.of(new PrincipalSid("oleg")));
        designRepositoryAclService.addPermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(AclPermission.READ, AclPermission.WRITE),
                List.of(new PrincipalSid("oleg")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(AclPermission.READ)));
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.READ)));
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(AclPermission.WRITE)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.move("repoId1", "/projectName1/rules", "/projectName1/rules1/rules2");

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules1/rules2", List.of(AclPermission.READ)));
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules1/rules2/module1.xlsx", List.of(AclPermission.READ)));
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules1/rules2/module1.xlsx", List.of(AclPermission.WRITE)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    public void nullPathSupport() {
        assertFalse(designRepositoryAclService.isGranted("repoId1", null, List.of(AclPermission.READ)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "", List.of(AclPermission.READ)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/", List.of(AclPermission.READ)));

        Authentication mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService
                .addPermissions("repoId1", null, List.of(AclPermission.READ), List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService.isGranted("repoId1", null, List.of(AclPermission.READ)));
        assertTrue(designRepositoryAclService.isGranted("repoId1", "", List.of(AclPermission.READ)));
        assertTrue(designRepositoryAclService.isGranted("repoId1", "/", List.of(AclPermission.READ)));

        setAdminAuthenticationToContext();
        designRepositoryAclService
                .removePermissions("repoId1", "", List.of(AclPermission.READ), List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertFalse(designRepositoryAclService.isGranted("repoId1", null, List.of(AclPermission.READ)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "", List.of(AclPermission.READ)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/", List.of(AclPermission.READ)));

        setAdminAuthenticationToContext();
        designRepositoryAclService
                .addPermissions("repoId1", "", List.of(AclPermission.READ), List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService.isGranted("repoId1", null, List.of(AclPermission.READ)));
        assertTrue(designRepositoryAclService.isGranted("repoId1", "", List.of(AclPermission.READ)));
        assertTrue(designRepositoryAclService.isGranted("repoId1", "/", List.of(AclPermission.READ)));

        setAdminAuthenticationToContext();
        designRepositoryAclService
                .removePermissions("repoId1", "/", List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertFalse(designRepositoryAclService.isGranted("repoId1", null, List.of(AclPermission.READ)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "", List.of(AclPermission.READ)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/", List.of(AclPermission.READ)));

        setAdminAuthenticationToContext();
        designRepositoryAclService
                .addPermissions("repoId1", "/", List.of(AclPermission.READ), List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService.isGranted("repoId1", null, List.of(AclPermission.READ)));
        assertTrue(designRepositoryAclService.isGranted("repoId1", "", List.of(AclPermission.READ)));
        assertTrue(designRepositoryAclService.isGranted("repoId1", "/", List.of(AclPermission.READ)));

        setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1", null);

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertFalse(designRepositoryAclService.isGranted("repoId1", null, List.of(AclPermission.READ)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "", List.of(AclPermission.READ)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/", List.of(AclPermission.READ)));

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
