package org.openl.rules.webstudio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.acls.domain.BasePermission;
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

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.workspace.lw.LocalWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;

@SpringJUnitConfig(classes = {DBTestConfiguration.class, AclServiceTestConfiguration.class})
@TestPropertySource(properties = {"db.url = jdbc:h2:mem:temp;DB_CLOSE_DELAY=-1",
        "db.user =",
        "db.password =",
        "db.maximumPoolSize = 3"})
@WithMockUser(value = "admin", authorities = "ADMIN")
class RepositoryAclServiceTest {

    private static final String DEVELOPERS_JUNIT = "DEVELOPERS_JUNIT";

    @Autowired
    @Qualifier("designRepositoryAclService")
    RepositoryAclService designRepositoryAclService;

    @Autowired
    SpringCacheBasedAclCache springCacheBasedAclCache;

    @BeforeEach
    void before() {
        springCacheBasedAclCache.clearCache();
    }

    @Test
    void exists() {
        assertNotNull(designRepositoryAclService);
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    void permissionChecking() {
        var mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.addPermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(BasePermission.WRITE),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.WRITE)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    void permissionMaskChecking() {
        var mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.addPermissions("repoId2",
                "/projectName1/rules",
                List.of(BasePermission.WRITE),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService
                .isGranted("repoId2", "/projectName1/rules/module1.xlsx", List.of(BasePermission.WRITE)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    void permissionInheritanceChecking() {
        var mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.addPermissions("repoId2",
                "/projectName1/rules",
                List.of(BasePermission.WRITE),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(
                designRepositoryAclService.isGranted("repoId2", "/projectName1/rules/module1.xlsx", List.of(BasePermission.WRITE)));
    }

    @Test
    @WithMockUser(value = "admin", authorities = "ADMIN")
    @Transactional
    @Rollback
    void permissionDuplicateChecking() {
        designRepositoryAclService.addPermissions("repoId2",
                "/projectName1/rules",
                List.of(BasePermission.WRITE),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));
        designRepositoryAclService.addPermissions("repoId2",
                "/projectName1/rules",
                List.of(BasePermission.WRITE),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));
        designRepositoryAclService.addPermissions("repoId2",
                "/projectName1/rules",
                List.of(BasePermission.WRITE),
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
    void permissionRemovingByPermission() {
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.READ)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.WRITE)));

        var mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.addPermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(BasePermission.WRITE, BasePermission.READ),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.READ)));
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.WRITE)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(BasePermission.WRITE),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.READ)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.WRITE)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(BasePermission.READ),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.READ)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.WRITE)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    void permissionRemovingBySids() {
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.READ)));
        var mockUser = setAndreyAuthenticationToContext();
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.READ)));

        setAdminAuthenticationToContext();
        designRepositoryAclService.addPermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(BasePermission.READ),
                List.of(new PrincipalSid("oleg"), new PrincipalSid("andrey")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.READ)));

        setAndreyAuthenticationToContext();
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.READ)));

        setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(BasePermission.READ),
                List.of(new PrincipalSid("andrey")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.READ)));

        setAndreyAuthenticationToContext();
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.READ)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(BasePermission.READ),
                List.of(new PrincipalSid("oleg")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.READ)));
        setAndreyAuthenticationToContext();
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.READ)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Rollback
    @Transactional
    void delete() {
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(BasePermission.READ)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.READ)));

        var mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService
                .addPermissions("repoId1", "/projectName1/rules", List.of(BasePermission.READ), List.of(new PrincipalSid("oleg")));
        designRepositoryAclService.addPermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(BasePermission.READ),
                List.of(new PrincipalSid("oleg")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(BasePermission.READ)));
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.READ)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.deleteAcl("repoId1", "/projectName1/rules");

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(BasePermission.READ)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.READ)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Rollback
    @Transactional
    void move() {
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(BasePermission.READ)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.READ)));
        assertFalse(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.WRITE)));

        var mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService
                .addPermissions("repoId1", "/projectName1/rules", List.of(BasePermission.READ), List.of(new PrincipalSid("oleg")));
        designRepositoryAclService.addPermissions("repoId1",
                "/projectName1/rules/module1.xlsx",
                List.of(BasePermission.READ, BasePermission.WRITE),
                List.of(new PrincipalSid("oleg")));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules", List.of(BasePermission.READ)));
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.READ)));
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules/module1.xlsx", List.of(BasePermission.WRITE)));

        mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.move("repoId1", "/projectName1/rules", "/projectName1/rules1/rules2");

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService.isGranted("repoId1", "/projectName1/rules1/rules2", List.of(BasePermission.READ)));
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules1/rules2/module1.xlsx", List.of(BasePermission.READ)));
        assertTrue(
                designRepositoryAclService.isGranted("repoId1", "/projectName1/rules1/rules2/module1.xlsx", List.of(BasePermission.WRITE)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    void nullPathSupport() {
        assertFalse(designRepositoryAclService.isGranted("repoId1", null, List.of(BasePermission.READ)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "", List.of(BasePermission.READ)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/", List.of(BasePermission.READ)));

        var mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService
                .addPermissions("repoId1", null, List.of(BasePermission.READ), List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService.isGranted("repoId1", null, List.of(BasePermission.READ)));
        assertTrue(designRepositoryAclService.isGranted("repoId1", "", List.of(BasePermission.READ)));
        assertTrue(designRepositoryAclService.isGranted("repoId1", "/", List.of(BasePermission.READ)));

        setAdminAuthenticationToContext();
        designRepositoryAclService
                .removePermissions("repoId1", "", List.of(BasePermission.READ), List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertFalse(designRepositoryAclService.isGranted("repoId1", null, List.of(BasePermission.READ)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "", List.of(BasePermission.READ)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/", List.of(BasePermission.READ)));

        setAdminAuthenticationToContext();
        designRepositoryAclService
                .addPermissions("repoId1", "", List.of(BasePermission.READ), List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService.isGranted("repoId1", null, List.of(BasePermission.READ)));
        assertTrue(designRepositoryAclService.isGranted("repoId1", "", List.of(BasePermission.READ)));
        assertTrue(designRepositoryAclService.isGranted("repoId1", "/", List.of(BasePermission.READ)));

        setAdminAuthenticationToContext();
        designRepositoryAclService
                .removePermissions("repoId1", "/", List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertFalse(designRepositoryAclService.isGranted("repoId1", null, List.of(BasePermission.READ)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "", List.of(BasePermission.READ)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/", List.of(BasePermission.READ)));

        setAdminAuthenticationToContext();
        designRepositoryAclService
                .addPermissions("repoId1", "/", List.of(BasePermission.READ), List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertTrue(designRepositoryAclService.isGranted("repoId1", null, List.of(BasePermission.READ)));
        assertTrue(designRepositoryAclService.isGranted("repoId1", "", List.of(BasePermission.READ)));
        assertTrue(designRepositoryAclService.isGranted("repoId1", "/", List.of(BasePermission.READ)));

        setAdminAuthenticationToContext();
        designRepositoryAclService.removePermissions("repoId1", null);

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        assertFalse(designRepositoryAclService.isGranted("repoId1", null, List.of(BasePermission.READ)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "", List.of(BasePermission.READ)));
        assertFalse(designRepositoryAclService.isGranted("repoId1", "/", List.of(BasePermission.READ)));

    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    void filterGrantedAnswersEveryArtefactAndAgreesWithTheSingleQuestion() {
        var mockUser = setAdminAuthenticationToContext();
        designRepositoryAclService.addPermissions("repoId1",
                "/readable",
                List.of(BasePermission.READ),
                List.of(new GrantedAuthoritySid(DEVELOPERS_JUNIT)));

        SecurityContextHolder.getContext().setAuthentication(mockUser);
        var readableOnMain = project("repoId1", "/readable");
        var readableOnFeature = project("repoId1", "/readable");
        var denied = project("repoId1", "/denied");

        var granted = designRepositoryAclService.filterGranted(
                List.of(readableOnMain, readableOnFeature, denied), List.of(BasePermission.READ));

        // The two branch views share one identity, so both are answered, and the denied one is left out.
        assertEquals(2, granted.size());
        assertTrue(granted.contains(readableOnMain));
        assertTrue(granted.contains(readableOnFeature));
        assertFalse(granted.contains(denied));


        // The batch answers exactly what the single-artefact question answers.
        assertTrue(designRepositoryAclService.isGranted(readableOnMain, List.of(BasePermission.READ)));
        assertTrue(designRepositoryAclService.isGranted(readableOnFeature, List.of(BasePermission.READ)));
        assertFalse(designRepositoryAclService.isGranted(denied, List.of(BasePermission.READ)));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    void filterGrantedOnNoArtefactsGrantsNothing() {
        assertTrue(designRepositoryAclService.filterGranted(List.of(), List.of(BasePermission.READ)).isEmpty());
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    void filterGrantedAnswersForAProjectOfTheUsersOwnWorkspaceWithoutAsking() {
        var local = project(LocalWorkspace.LOCAL_ID, "/my-project");

        var granted = designRepositoryAclService.filterGranted(
                Collections.singletonList(local), List.of(BasePermission.READ));

        assertTrue(granted.contains(local));
    }

    @Test
    @WithMockUser(value = "oleg", authorities = DEVELOPERS_JUNIT)
    @Transactional
    @Rollback
    void filterGrantedRefusesAnArtefactThatIsNotThere() {
        assertTrue(designRepositoryAclService
                .filterGranted(Collections.singletonList(null), List.of(BasePermission.READ))
                .isEmpty());
    }

    private static AProject project(String repositoryId, String path) {
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn(repositoryId);
        var fileData = new FileData();
        fileData.setName(path);
        return new AProject(repository, fileData);
    }

    private Authentication setAdminAuthenticationToContext() {
        var currentUser = SecurityContextHolder.getContext().getAuthentication();
        var principal = SimpleUser.builder()
                .setUsername("admin")
                .setPrivileges(List.of(Privileges.ADMIN))
                .build();
        var auth = new UsernamePasswordAuthenticationToken(principal,
                "password",
                principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return currentUser;
    }

    private Authentication setAndreyAuthenticationToContext() {
        var currentUser = SecurityContextHolder.getContext().getAuthentication();
        var group = new SimpleGroup();
        group.setName(DEVELOPERS_JUNIT);
        var principal = SimpleUser.builder().setUsername("andrey").setPrivileges(List.of(group)).build();
        var auth = new UsernamePasswordAuthenticationToken(principal,
                "password",
                principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return currentUser;
    }
}
