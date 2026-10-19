package org.openl.rules.rest.acl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclRoleAuditor.Place;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.AclRepositoryType;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditSubjectType;
import org.openl.studio.audit.model.AuditTargetType;

class AclRoleAuditorTest {

    private static final PrincipalSid JDOE = new PrincipalSid("jdoe");
    private static final GrantedAuthoritySid TESTERS = new GrantedAuthoritySid("Testers");

    private final List<Object> events = new ArrayList<>();
    private final AclRoleAuditor auditor = new AclRoleAuditor(events::add);

    @Test
    void grantedRole_namesTheUserAndThePlace() {
        auditor.announce(Place.repository(AclRepositoryType.DESIGN, "design-repo"), JDOE, null, "VIEWER");

        var event = single();
        assertEquals(AuditAction.ROLE_GRANTED, event.action());
        assertEquals(AuditTargetType.REPOSITORY, event.targetType());
        assertEquals("design-repo", event.target());
        assertEquals(AuditSubjectType.USER, event.subjectType());
        assertEquals("jdoe", event.subject());
        assertEquals(Map.of("type", "design", "to", "VIEWER"), event.details());
    }

    @Test
    void changedRole_keepsBothRoles() {
        auditor.announce(Place.root(AclRepositoryType.PROD), TESTERS, "VIEWER", "MANAGER");

        var event = single();
        assertEquals(AuditAction.ROLE_CHANGED, event.action());
        assertEquals(AuditTargetType.ROOT, event.targetType());
        assertEquals("prod", event.target());
        assertEquals(AuditSubjectType.GROUP, event.subjectType());
        assertEquals("Testers", event.subject());
        assertEquals(Map.of("from", "VIEWER", "to", "MANAGER"), event.details());
    }

    @Test
    void revokedRole_keepsTheRoleTakenAway() {
        var project = mock(AProject.class);
        var repository = mock(Repository.class);
        when(project.getBusinessName()).thenReturn("Pricing");
        when(project.getRepository()).thenReturn(repository);
        when(repository.getId()).thenReturn("design");
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());

        auditor.announce(Place.project(project), TESTERS, "CONTRIBUTOR", null);

        var event = single();
        assertEquals(AuditAction.ROLE_REVOKED, event.action());
        assertEquals(AuditTargetType.PROJECT, event.targetType());
        assertEquals("Pricing", event.target());
        assertEquals(Map.of("repository", "design", "from", "CONTRIBUTOR"), event.details());
    }

    @Test
    void projectOfARepositoryWithFoldersOfItsOwn_isNamedWithItsFolder() {
        var project = mock(RulesProject.class);
        var workspace = mock(Repository.class);
        var design = mock(Repository.class);
        when(project.getBusinessName()).thenReturn("Pricing");
        when(project.getRepository()).thenReturn(workspace);
        when(project.getDesignRepository()).thenReturn(design);
        when(project.getRealPath()).thenReturn("sales\\Pricing");
        when(workspace.getId()).thenReturn("design");
        when(design.supports()).thenReturn(new FeaturesBuilder(design).setMappedFolders(true).build());

        auditor.announce(Place.project(project), JDOE, null, "VIEWER");

        assertEquals(Map.of("repository", "design", "path", "sales/Pricing", "to", "VIEWER"), single().details());
    }

    @Test
    void sameRole_announcesNothing() {
        auditor.announce(Place.root(AclRepositoryType.DESIGN), JDOE, "VIEWER", "VIEWER");
        auditor.announce(Place.root(AclRepositoryType.DESIGN), JDOE, null, null);

        assertTrue(events.isEmpty());
    }

    @Test
    void rolesOfAPlace_areComparedBySubject() {
        var bob = new PrincipalSid("bob");
        auditor.announce(Place.root(AclRepositoryType.DESIGN),
                Map.of(JDOE, "VIEWER", TESTERS, "CONTRIBUTOR", bob, "MANAGER"),
                Map.of(JDOE, "MANAGER", bob, "MANAGER", new GrantedAuthoritySid("Analysts"), "VIEWER"));

        var announced = events.stream()
                .map(AuditEvent.class::cast)
                .map(event -> event.action() + " " + event.subject())
                .sorted()
                .toList();
        assertEquals(List.of("ROLE_CHANGED jdoe", "ROLE_GRANTED Analysts", "ROLE_REVOKED Testers"), announced);
    }

    @Test
    void roleOf_namesTheRoleOfThePermissions() {
        assertNull(AclRoleAuditor.roleOf(List.of()));
        assertEquals("MANAGER", AclRoleAuditor.roleOf(List.of(AclRole.MANAGER.getCumulativePermission())));
        assertEquals("mask 2", AclRoleAuditor.roleOf(List.of(BasePermission.WRITE)));
        assertEquals("VIEWER, mask 2",
                AclRoleAuditor.roleOf(List.of(AclRole.VIEWER.getCumulativePermission(), BasePermission.WRITE)));
    }

    @Test
    void rolesOf_leavesOutSubjectsWithoutPermissions() {
        var roles = AclRoleAuditor.rolesOf(Map.of(
                JDOE, List.of(AclRole.CONTRIBUTOR.getCumulativePermission()),
                TESTERS, List.of()));

        assertEquals(Map.of(JDOE, "CONTRIBUTOR"), roles);
    }

    private AuditEvent single() {
        assertEquals(1, events.size());
        return assertInstanceOf(AuditEvent.class, events.getFirst());
    }
}
