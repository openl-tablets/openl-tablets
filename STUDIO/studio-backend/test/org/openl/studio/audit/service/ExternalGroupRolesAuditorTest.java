package org.openl.studio.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.model.Permission;

import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclRoleAuditor;
import org.openl.rules.webstudio.security.SecureDesignTimeRepository;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.security.acl.repository.SimpleRepositoryAclService;
import org.openl.security.acl.repository.SimpleRepositoryAclService.Location;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditSubjectType;

class ExternalGroupRolesAuditorTest {

    private static final GrantedAuthoritySid ANALYSTS = new GrantedAuthoritySid("Analysts");

    private final List<AuditEvent> events = new ArrayList<>();
    private final RepositoryAclService designAclService = mock(RepositoryAclService.class);
    private final SimpleRepositoryAclService productionAclService = mock(SimpleRepositoryAclService.class);
    private final SecureDesignTimeRepository designTimeRepository = mock(SecureDesignTimeRepository.class);
    private ExternalGroupRolesAuditor auditor;

    @BeforeEach
    void setUp() {
        var aclServiceProvider = mock(RepositoryAclServiceProvider.class);
        when(aclServiceProvider.getAclService("design")).thenReturn(designAclService);
        when(aclServiceProvider.getAclService("prod")).thenReturn(productionAclService);
        auditor = new ExternalGroupRolesAuditor(aclServiceProvider, designTimeRepository,
                new AclRoleAuditor(event -> events.add((AuditEvent) event)));
    }

    @Test
    void groupJoined_grantsTheUserEveryRoleOfTheGroup() {
        var flat = mock(Repository.class);
        when(designTimeRepository.getRepository("design")).thenReturn(flat);
        when(designAclService.listAllPermissions(ANALYSTS)).thenReturn(held(
                Map.entry(Location.ROOT, AclRole.VIEWER),
                Map.entry(new Location("design", null), AclRole.CONTRIBUTOR),
                Map.entry(new Location("design", "DESIGN/rules/Pricing"), AclRole.MANAGER)));
        when(productionAclService.listAllPermissions(ANALYSTS)).thenReturn(held(
                Map.entry(new Location("production", null), AclRole.VIEWER),
                Map.entry(new Location("production", "deployments/Pricing"), AclRole.VIEWER)));

        auditor.announce("jdoe", Set.of("Analysts"), Set.of());

        assertEquals(List.of(
                "ROLE_GRANTED ROOT design",
                "ROLE_GRANTED REPOSITORY design",
                "ROLE_GRANTED PROJECT Pricing",
                "ROLE_GRANTED REPOSITORY production"), announced(), "A path in a production repository holds no role");
        assertEquals(List.of(
                Map.of("to", "VIEWER"),
                Map.of("type", "design", "to", "CONTRIBUTOR"),
                Map.of("repository", "design", "to", "MANAGER"),
                Map.of("type", "prod", "to", "VIEWER")), placesAndRoles());
    }

    @Test
    void groupLeft_revokesFromTheUserEveryRoleOfTheGroup() {
        when(designAclService.listAllPermissions(ANALYSTS))
                .thenReturn(held(Map.entry(new Location("design", null), AclRole.CONTRIBUTOR)));

        auditor.announce("jdoe", Set.of(), Set.of("Analysts"));

        assertEquals(List.of("ROLE_REVOKED REPOSITORY design"), announced());
        assertEquals(List.of(Map.of("type", "design", "from", "CONTRIBUTOR")), placesAndRoles());
    }

    @Test
    void projectOfARepositoryWithFoldersOfItsOwn_isNamedAsItsMappingNamesIt() {
        var git = mock(Repository.class, withSettings().extraInterfaces(FolderMapper.class));
        var mapper = (FolderMapper) git;
        when(designTimeRepository.getRepository("git")).thenReturn(git);
        when(mapper.findMappedName("sales/pricing")).thenReturn("DESIGN/rules/Pricing Rules:3f2a");
        when(mapper.getBusinessName("DESIGN/rules/Pricing Rules:3f2a")).thenReturn("DESIGN/rules/Pricing Rules");
        when(designAclService.listAllPermissions(ANALYSTS)).thenReturn(held(
                Map.entry(new Location("git", "sales/pricing"), AclRole.VIEWER),
                Map.entry(new Location("git", "sales/unmapped"), AclRole.VIEWER)));

        auditor.announce("jdoe", Set.of("Analysts"), Set.of());

        assertEquals(List.of("ROLE_GRANTED PROJECT Pricing Rules", "ROLE_GRANTED PROJECT unmapped"), announced(),
                "A folder the mapping does not know is named by itself");
        assertEquals(List.of(
                Map.of("repository", "git", "path", "sales/pricing", "to", "VIEWER"),
                Map.of("repository", "git", "path", "sales/unmapped", "to", "VIEWER")), placesAndRoles());
    }

    @Test
    void groupWithoutRoles_announcesNothing() {
        when(designAclService.listAllPermissions(ANALYSTS)).thenReturn(Map.of());
        when(productionAclService.listAllPermissions(ANALYSTS)).thenReturn(Map.of());

        auditor.announce("jdoe", Set.of("Analysts"), Set.of("Analysts"));

        assertTrue(events.isEmpty());
    }

    /**
     * Each event as its action, the kind of its place and the name of the place.
     */
    private List<String> announced() {
        return events.stream()
                .map(event -> event.action() + " " + event.targetType() + " " + event.target())
                .toList();
    }

    /**
     * The details of each event but the group and the origin, which every event names alike for the user.
     */
    private List<Map<String, ?>> placesAndRoles() {
        return events.stream().<Map<String, ?>>map(event -> {
            assertEquals(AuditSubjectType.USER, event.subjectType());
            assertEquals("jdoe", event.subject());
            var details = new LinkedHashMap<String, Object>(event.details());
            assertEquals("Analysts", details.remove("group"));
            assertEquals("identityProvider", details.remove("origin"));
            return details;
        }).toList();
    }

    /**
     * The roles a group holds, by where they are held, in the order given.
     */
    @SafeVarargs
    private static Map<Location, List<Permission>> held(Map.Entry<Location, AclRole>... roles) {
        var held = new LinkedHashMap<Location, List<Permission>>();
        for (var role : roles) {
            held.put(role.getKey(), List.of(role.getValue().getCumulativePermission()));
        }
        return held;
    }
}
