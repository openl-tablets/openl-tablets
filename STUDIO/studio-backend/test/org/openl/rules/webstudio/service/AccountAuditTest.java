package org.openl.rules.webstudio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.security.Group;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.webstudio.service.config.UserManagementConfiguration;
import org.openl.studio.audit.AuditEventsCollector;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditSubjectType;
import org.openl.studio.audit.model.AuditTargetType;
import org.openl.studio.common.exception.NotFoundException;

/**
 * The changes of users and groups are announced to the audit log, and only the changes.
 */
@SpringJUnitConfig(classes = {DBTestConfiguration.class,
        UserManagementConfiguration.class,
        AclServiceTestConfiguration.class,
        AccountAuditTest.Config.class})
@TestPropertySource(properties = {"db.url = jdbc:h2:mem:account-audit;DB_CLOSE_DELAY=-1",
        "db.user =",
        "db.password =",
        "db.maximumPoolSize = 3"})
class AccountAuditTest {

    @Configuration
    static class Config {

        @Bean
        AuditEventsCollector auditEventsCollector() {
            return new AuditEventsCollector();
        }
    }

    @Autowired
    private UserManagementService userService;

    @Autowired
    private GroupManagementService groupService;

    @Autowired
    private ExternalGroupService externalGroupService;

    @Autowired
    private GroupDao groupDao;

    @Autowired
    private AuditEventsCollector audit;

    @Autowired
    @Qualifier("flywayDBReset")
    private Flyway flywayDBReset;

    @BeforeEach
    void setUp() {
        flywayDBReset.clean();
        flywayDBReset.migrate();
        audit.clear();
    }

    @Test
    void announcesAUserCreatedChangedAndDeleted() {
        userService.addUser("jdoe", "John", "Doe", "secret", "jdoe@example.com", "John Doe");
        userService.updateUserData("jdoe", "John", "Doe", null, "jdoe@example.com", "John Doe", false);
        userService.updateUserData("jdoe", "Johnny", "Doe", "", "john.doe@example.com", "John Doe", true);
        userService.updateUserData("jdoe", "Johnny", "Doe", "new-secret", "john.doe@example.com", "John Doe", true);
        userService.deleteUser("jdoe");

        assertEquals(List.of(AuditAction.USER_CREATED, AuditAction.USER_UPDATED, AuditAction.PASSWORD_CHANGED,
                AuditAction.USER_DELETED), actions(), "A save that keeps every value announces nothing");
        audit.events().forEach(event -> {
            assertEquals(AuditTargetType.USER, event.targetType());
            assertEquals("jdoe", event.target());
        });
        assertEquals(Map.of("changed", Map.of(
                        "firstName", change("John", "Johnny"),
                        "email", change("jdoe@example.com", "john.doe@example.com"),
                        "emailVerified", change(false, true))),
                audit.events(AuditAction.USER_UPDATED).getFirst().details());
        assertTrue(audit.events(AuditAction.PASSWORD_CHANGED).getFirst().details().isEmpty(),
                "A password is never put into the log");
    }

    @Test
    void announcesTheGroupsAUserJoinedAndLeft() {
        // Analysts and Testers are among the groups a new database starts with.
        userService.addUser("jdoe", null, null, null, null, null);
        audit.clear();

        userService.updateAuthorities("jdoe", Set.of("Analysts", "Unknown"));
        userService.updateAuthorities("jdoe", Set.of("Testers"), false);

        var events = audit.events();
        assertEquals(List.of(
                membership(AuditAction.GROUP_MEMBER_ADDED, "Analysts"),
                membership(AuditAction.GROUP_MEMBER_ADDED, "Testers"),
                membership(AuditAction.GROUP_MEMBER_REMOVED, "Analysts")), events);
    }

    @Test
    void announcesAUserTheIdentityProviderCreatedOrChanged() {
        userService.syncUserData("jdoe", "John", "Doe", "jdoe@example.com", "John Doe");
        userService.syncUserData("jdoe", "John", "Doe", "jdoe@example.com", "John Doe");
        userService.syncUserData("jdoe", "John", "Smith", "jdoe@example.com", "John Smith");

        assertEquals(List.of(AuditAction.USER_CREATED, AuditAction.USER_UPDATED), actions(),
                "A sign-in that changes nothing announces nothing");
        assertEquals(Map.of("origin", "identityProvider"), audit.events().getFirst().details());
        assertEquals(Map.of("origin", "identityProvider", "changed", Map.of(
                        "lastName", change("Doe", "Smith"),
                        "displayName", change("John Doe", "John Smith"))),
                audit.events().get(1).details());
    }

    @Test
    void announcesAGroupCreatedChangedAndDeleted() {
        groupService.addGroup("Pricing", "Read the rules");
        groupService.updateGroup("Pricing", "Pricing", "Read the rules");
        groupService.updateGroup("Pricing", "Billing", "Review the rules");
        groupService.updateGroup("Billing", Set.of(Privileges.ADMIN.getAuthority()));
        groupService.updateGroup("Billing", Set.of(Privileges.ADMIN.getAuthority()));
        groupService.updateGroup("Billing", Set.of());
        groupService.deleteGroup(groupDao.getGroupByName("Billing").getId());

        assertEquals(List.of(AuditAction.GROUP_CREATED, AuditAction.GROUP_UPDATED,
                AuditAction.GROUP_PRIVILEGES_CHANGED, AuditAction.GROUP_PRIVILEGES_CHANGED,
                AuditAction.GROUP_DELETED), actions());
        var events = audit.events();
        assertEquals("Pricing", events.getFirst().target());
        assertEquals(Map.of("changed", Map.of(
                        "name", change("Pricing", "Billing"),
                        "description", change("Read the rules", "Review the rules"))),
                events.get(1).details());
        assertEquals(Map.of("added", Set.of("ADMIN"), "removed", Set.of()), events.get(2).details());
        assertEquals(Map.of("added", Set.of(), "removed", Set.of("ADMIN")), events.get(3).details());
        assertEquals("Billing", events.get(4).target());
        events.forEach(event -> assertEquals(AuditTargetType.GROUP, event.targetType()));
    }

    @Test
    void findsNoGroupToChangeByAnUnknownName() {
        assertThrows(NotFoundException.class, () -> groupService.updateGroup("Unknown", "Other", "Read the rules"));
        assertThrows(NotFoundException.class, () -> groupService.updateGroup("Unknown", Set.of()));

        assertTrue(audit.events().isEmpty());
    }

    @Test
    void announcesTheExternalGroupsThatChanged() {
        userService.addUser("jdoe", null, null, null, null, null);
        audit.clear();

        externalGroupService.mergeAllForUser("jdoe", List.of(authority("Analysts"), authority("Testers")));
        externalGroupService.mergeAllForUser("jdoe", List.of(authority("Testers"), authority("Analysts")));
        externalGroupService.mergeAllForUser("jdoe", List.of(authority("Testers"), authority("Deployers")));

        var events = audit.events();
        assertEquals(2, events.size(), "The same groups in another order change nothing");
        assertEquals(AuditAction.EXTERNAL_GROUPS_CHANGED, events.getFirst().action());
        assertEquals("jdoe", events.getFirst().target());
        assertEquals(Map.of("added", Set.of("Analysts", "Testers"), "removed", Set.of()),
                events.getFirst().details());
        assertEquals(Map.of("added", Set.of("Deployers"), "removed", Set.of("Analysts")), events.get(1).details());
        assertEquals(Set.of("Testers", "Deployers"), externalGroupService.findAllForUser("jdoe")
                .stream()
                .map(Group::getAuthority)
                .collect(Collectors.toSet()), "The groups the identity provider reports last are kept");
    }

    private List<AuditAction> actions() {
        return audit.events().stream().map(AuditEvent::action).toList();
    }

    private static Map<String, Object> change(Object from, Object to) {
        return Map.of("from", from, "to", to);
    }

    private static SimpleGrantedAuthority authority(String name) {
        return new SimpleGrantedAuthority(name);
    }

    private static AuditEvent membership(AuditAction action, String group) {
        return AuditEvent.builder()
                .action(action)
                .targetType(AuditTargetType.GROUP)
                .target(group)
                .subjectType(AuditSubjectType.USER)
                .subject("jdoe")
                .build();
    }
}
