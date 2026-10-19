package org.openl.studio.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.openl.rules.repository.api.Offset;
import org.openl.rules.security.standalone.dao.AuditRecordDao;
import org.openl.rules.security.standalone.persistence.AuditRecord;
import org.openl.rules.webstudio.service.DBTestConfiguration;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditAuthentication;
import org.openl.studio.audit.model.AuditCategory;
import org.openl.studio.audit.model.AuditEvent;
import org.openl.studio.audit.model.AuditEventView;
import org.openl.studio.audit.model.AuditOutcome;
import org.openl.studio.audit.model.AuditQuery;
import org.openl.studio.audit.model.AuditSubjectType;
import org.openl.studio.audit.model.AuditTargetType;
import org.openl.studio.security.pat.model.PatAuthenticationToken;

@SpringJUnitConfig(classes = {DBTestConfiguration.class, AuditServiceTest.Config.class})
@TestPropertySource(properties = {"db.url = jdbc:h2:mem:audit;DB_CLOSE_DELAY=-1",
        "db.user =",
        "db.password =",
        "db.maximumPoolSize = 3",
        "user.mode = multi",
        "audit.outputs.console = true"})
class AuditServiceTest {

    private static final AuditQuery ALL = AuditQuery.builder().build();

    @Configuration
    static class Config {

        @Bean
        AuditService auditService(AuditRecordDao auditRecordDao) {
            return new AuditService(auditRecordDao, "multi", true);
        }
    }

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditRecordDao auditRecordDao;

    @Autowired
    @Qualifier("flywayDBReset")
    private Flyway flywayDBReset;

    @BeforeEach
    void setUp() {
        flywayDBReset.clean();
        flywayDBReset.migrate();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordsTheSignedInUserAndTheClientAddress() {
        signIn("jdoe");
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.7");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        auditService.recordEvent(AuditEvent.builder()
                .action(AuditAction.SIGN_OUT)
                .details(Map.of("reason", "expired"))
                .build());

        var event = only(all());
        assertEquals("jdoe", event.actor());
        assertEquals(AuditAuthentication.FORM, event.authentication());
        assertEquals("10.0.0.7", event.clientAddress());
        assertEquals(AuditCategory.AUTHENTICATION, event.category());
        assertEquals(AuditAction.SIGN_OUT, event.action());
        assertEquals(AuditOutcome.SUCCESS, event.outcome());
        assertEquals(Map.of("reason", "expired"), event.details());
        assertTrue(event.time().isAfter(Instant.now().minusSeconds(60)));
    }

    @Test
    void recordsHowTheSignedInUserSignedIn() {
        SecurityContextHolder.getContext().setAuthentication(new PatAuthenticationToken("jdoe", null, List.of(),
                "abc123DEF4567890"));

        auditService.recordEvent(AuditEvent.builder().action(AuditAction.AUDIT_LOG_READ).build());

        assertEquals(AuditAuthentication.TOKEN, only(all()).authentication());
    }

    @Test
    void recordsAnActionRepeatedWithNothingElseInBetweenOnce() {
        signIn("jdoe");
        var read = AuditEvent.builder().action(AuditAction.AUDIT_LOG_READ).build();

        auditService.recordUnlessRepeated(read);
        auditService.recordUnlessRepeated(read);
        // Another user does not end the repetition.
        auditService.recordEvent(AuditEvent.builder().action(AuditAction.SIGN_OUT).actor("admin").build());
        auditService.recordUnlessRepeated(read);
        // Anything else the user does ends it.
        auditService.recordEvent(AuditEvent.builder().action(AuditAction.SIGN_IN).build());
        auditService.recordUnlessRepeated(read);

        assertEquals(List.of(AuditAction.AUDIT_LOG_READ, AuditAction.SIGN_IN, AuditAction.SIGN_OUT,
                AuditAction.AUDIT_LOG_READ), all().stream().map(AuditEventView::action).toList());
    }

    @Test
    void findsTheEventsOfOneWayOfSigningIn() {
        signIn("jdoe");
        auditService.recordEvent(AuditEvent.builder().action(AuditAction.AUDIT_LOG_READ).build());
        auditService.recordEvent(AuditEvent.builder()
                .action(AuditAction.SIGN_IN)
                .actor("agent")
                .authentication(AuditAuthentication.TOKEN)
                .build());

        var tokens = AuditQuery.builder().authentication(AuditAuthentication.TOKEN).build();
        assertEquals(List.of("agent"), actors(tokens));
    }

    @Test
    void namesTheActorTheEventNames() {
        signIn("admin");

        auditService.recordEvent(AuditEvent.builder()
                .action(AuditAction.SIGN_IN)
                .outcome(AuditOutcome.FAILURE)
                .actor("jdoe")
                .build());

        var event = only(all());
        assertEquals("jdoe", event.actor());
        assertNull(event.authentication(), "The signed-in user is not the one the event names");
        assertEquals(AuditOutcome.FAILURE, event.outcome());
    }

    @Test
    void namesNoActorForAnActionOfStudioItself() {
        auditService.recordEvent(AuditEvent.builder().action(AuditAction.AUDIT_LOG_PURGED).build());
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key",
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        auditService.recordEvent(AuditEvent.builder().action(AuditAction.AUDIT_LOG_PURGED).build());

        all().forEach(event -> {
            assertNull(event.actor());
            assertNull(event.clientAddress());
            assertTrue(event.details().isEmpty());
        });
    }

    @Test
    void leavesOutDetailsWithoutValueAndKeepsALongTargetAndDetailsWhole() {
        var details = new HashMap<String, Object>();
        details.put("kept", "value");
        details.put("dropped", null);
        var longTarget = "t".repeat(100_000) + ", mail.url";
        auditService.recordEvent(AuditEvent.builder()
                .action(AuditAction.AUDIT_LOG_READ)
                .targetType(AuditTargetType.PROJECT)
                .target(longTarget)
                .details(details)
                .build());
        var longValue = "d".repeat(100_000);
        auditService.recordEvent(AuditEvent.builder()
                .action(AuditAction.AUDIT_LOG_READ)
                .details(Map.of("long", longValue))
                .build());

        var events = all();
        assertEquals(Map.of("long", longValue), events.getFirst().details());
        var whole = events.get(1);
        assertEquals(Map.of("kept", "value"), whole.details());
        assertEquals(longTarget, whole.target());
        assertEquals(1, events(AuditQuery.builder().target("MAIL.URL").build()).size(),
                "A long target is searched whole");
    }

    @Test
    void readsDetailsThatAreNoJsonObjectAsTheirText() {
        var broken = newRecord(Instant.now(), "admin", AuditAction.AUDIT_LOG_READ, AuditOutcome.SUCCESS, null, null);
        broken.setDetails("{broken");
        auditRecordDao.save(broken);

        assertEquals(Map.of("text", "{broken"), all().getFirst().details());
    }

    @Test
    void recordsNothingInSingleUserMode() {
        var singleUser = new AuditService(auditRecordDao, "single", false);

        singleUser.recordEvent(AuditEvent.builder().action(AuditAction.SIGN_IN).build());
        singleUser.recordUnlessRepeated(AuditEvent.builder().action(AuditAction.AUDIT_LOG_READ).build());

        assertTrue(all().isEmpty());
    }

    @Test
    void findsTheEventsTheCriteriaMatch() {
        // Whole seconds, as MySQL keeps the time: it rounds a fraction of a second.
        var now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        save(now.minus(3, ChronoUnit.DAYS), "Admin", AuditAction.SIGN_IN, AuditOutcome.SUCCESS, null, null);
        var refused = newRecord(now.minus(2, ChronoUnit.DAYS), "jdoe", AuditAction.SIGN_IN, AuditOutcome.FAILURE, null,
                null);
        refused.setClientAddress("10.0.0.7");
        auditRecordDao.save(refused);
        save(now.minus(1, ChronoUnit.DAYS), "admin", AuditAction.AUDIT_LOG_READ, AuditOutcome.SUCCESS,
                "design:/rules/100% Sales", "Analysts");

        assertEquals(List.of("jdoe"), actors(AuditQuery.builder()
                .from(now.minus(2, ChronoUnit.DAYS))
                .to(now.minus(1, ChronoUnit.DAYS))
                .build()));
        assertEquals(List.of("admin", "Admin"), actors(AuditQuery.builder().actor(" ADM ").build()));
        assertEquals(List.of("admin"), actors(AuditQuery.builder().categories(Set.of(AuditCategory.AUDIT)).build()));
        assertEquals(List.of("jdoe", "Admin"), actors(AuditQuery.builder()
                .categories(Set.of(AuditCategory.AUTHENTICATION, AuditCategory.AUDIT))
                .actions(Set.of(AuditAction.SIGN_IN, AuditAction.SIGN_OUT))
                .build()), "Both criteria match: the actions of the categories");
        assertEquals(List.of(), actors(AuditQuery.builder()
                .categories(Set.of(AuditCategory.AUDIT))
                .actions(Set.of(AuditAction.SIGN_IN))
                .build()), "No action of the category is asked for");
        assertEquals(List.of("jdoe", "Admin"), actors(AuditQuery.builder()
                .actions(Set.of(AuditAction.SIGN_IN))
                .build()));
        assertEquals(List.of("jdoe"), actors(AuditQuery.builder().outcome(AuditOutcome.FAILURE).build()));
        assertEquals(List.of("admin"), actors(AuditQuery.builder().target("100% s").build()));
        assertEquals(List.of(), actors(AuditQuery.builder().target("100_").build()), "Wildcards are plain text");
        assertEquals(List.of("admin"), actors(AuditQuery.builder().subject("analyst").build()));
        assertEquals(List.of("jdoe"), actors(AuditQuery.builder().clientAddress("10.0.0").build()));
        assertEquals(3, actors(AuditQuery.builder().actor(" ").build()).size(), "A blank text matches everything");
    }

    @Test
    void readsAPageOfTheNewestEvents() {
        var now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        IntStream.range(0, 5).forEach(i -> save(now.plusSeconds(i), "user" + i, AuditAction.SIGN_IN,
                AuditOutcome.SUCCESS, null, null));

        var page = auditService.find(ALL, Offset.of(1, 2));

        assertEquals(List.of("user3", "user2"), page.getContent().stream().map(AuditEventView::actor).toList());
        assertEquals(5L, page.getTotal());
        assertFalse(page.isTotalLimited());
        assertEquals(2, page.getPageSize());
        assertEquals(3, auditRecordDao.count(ALL, 3), "The count stops at its limit");
        assertEquals(5, auditRecordDao.count(ALL, 10));
    }

    @Test
    void countsAPageUpToTheLimitAndTellsWhetherMoreEventsMatch() throws JsonProcessingException {
        var dao = mock(AuditRecordDao.class);
        when(dao.find(ALL, 0, 20)).thenReturn(List.of());
        when(dao.count(ALL, AuditService.TOTAL_LIMIT + 1)).thenReturn(AuditService.TOTAL_LIMIT + 1L, 7L);
        var service = new AuditService(dao, "multi", false);

        var limited = service.find(ALL, Offset.of(0, 20));
        var counted = service.find(ALL, Offset.of(0, 20));

        assertEquals((long) AuditService.TOTAL_LIMIT, limited.getTotal());
        assertTrue(limited.isTotalLimited());
        assertEquals(7L, counted.getTotal());
        assertFalse(counted.isTotalLimited());
        var json = JsonMapper.builder().build();
        assertTrue(json.writeValueAsString(limited).contains("\"totalLimited\":true"));
        assertFalse(json.writeValueAsString(counted).contains("totalLimited"), "Left out while the total is exact");
    }

    @Test
    void walksTheWholeLogABatchAtATime() {
        var now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        var records = new ArrayList<AuditRecord>();
        for (var i = 0; i < 1201; i++) {
            records.add(newRecord(now, "user" + i, AuditAction.SIGN_IN, AuditOutcome.SUCCESS, null, null));
        }
        auditRecordDao.save(records);

        var ids = new ArrayList<Long>();
        auditService.forEach(ALL, event -> ids.add(event.id()));

        assertEquals(1201, ids.size());
        assertEquals(ids.stream().sorted((a, b) -> Long.compare(b, a)).toList(), ids, "Newest first");
        assertEquals(1201L, auditService.find(ALL, Offset.of(0, 1)).getTotal());
    }

    @Test
    void removesTheOldEventsABatchAtATime() {
        var now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        save(now.minus(12, ChronoUnit.DAYS), "oldest", AuditAction.SIGN_IN, AuditOutcome.SUCCESS, null, null);
        save(now.minus(11, ChronoUnit.DAYS), "older", AuditAction.SIGN_IN, AuditOutcome.SUCCESS, null, null);
        save(now.minus(10, ChronoUnit.DAYS), "old", AuditAction.SIGN_IN, AuditOutcome.SUCCESS, null, null);
        save(now.minus(1, ChronoUnit.DAYS), "recent", AuditAction.SIGN_IN, AuditOutcome.SUCCESS, null, null);
        var before = now.minus(5, ChronoUnit.DAYS);

        assertEquals(2, auditService.removeBefore(before, 2));
        assertEquals(List.of("old", "recent"), all().stream().map(AuditEventView::actor).toList().reversed(),
                "The oldest events go first");
        assertEquals(1, auditService.removeBefore(before, 2));
        assertEquals(0, auditService.removeBefore(before, 2), "Nothing more to remove");

        assertEquals(List.of("recent"), all().stream().map(AuditEventView::actor).toList(),
                "A batch records nothing: the retention records the whole removal");
    }

    @Test
    void describesAnEventForTheServerLog() {
        var full = newRecord(Instant.EPOCH, "jdoe", AuditAction.SIGN_IN, AuditOutcome.FAILURE, "design:/rules/X",
                "Analysts");
        full.setId(7L);
        full.setClientAddress("10.0.0.7");
        full.setAuthentication(AuditAuthentication.FORM);
        full.setDetails("{\"reason\":\"badCredentials\"}");
        var bare = newRecord(Instant.EPOCH, null, AuditAction.AUDIT_LOG_PURGED, AuditOutcome.SUCCESS, null, null);

        assertEquals("""
                signIn failure id=7 actor=jdoe authentication=form clientAddress=10.0.0.7 targetType=project\
                 target=design:/rules/X subjectType=group subject=Analysts details={"reason":"badCredentials"}\
                """, AuditService.describe(AuditService.fieldsOf(full)));
        assertEquals("auditLogPurged success", AuditService.describe(AuditService.fieldsOf(bare)));
    }

    @Test
    void namesTheFieldsOfAnEventForAStructuredServerLog() {
        var event = newRecord(Instant.EPOCH, "jdoe", AuditAction.SIGN_IN, AuditOutcome.FAILURE, null, null);
        event.setId(7L);

        assertEquals(List.of("id", "category", "action", "outcome", "actor"),
                List.copyOf(AuditService.fieldsOf(event).keySet()), "The columns of an export, in their order");
        assertEquals(Map.of("id", "7", "category", "authentication", "action", "signIn", "outcome", "failure",
                "actor", "jdoe"), AuditService.fieldsOf(event));
    }

    @Test
    void escapesTheLineBreaksAClientTypedIntoTheServerLog() {
        // A line separator of Unicode and a bell, built so that no escape of the source breaks its line.
        var typed = "x\r\nSIGN_IN SUCCESS actor=admin" + Character.toString(0x2028) + Character.toString(7);
        var forged = newRecord(Instant.EPOCH, typed, AuditAction.SIGN_IN, AuditOutcome.FAILURE, null, null);

        assertEquals("signIn failure actor=x\\r\\nSIGN_IN SUCCESS actor=admin\\u2028\\u0007",
                AuditService.describe(AuditService.fieldsOf(forged)),
                "The name a client typed stays on the line of its event");
    }

    private void signIn(String name) {
        SecurityContextHolder.getContext()
                .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(name, null, List.of()));
    }

    private List<AuditEventView> all() {
        return events(ALL);
    }

    private List<AuditEventView> events(AuditQuery query) {
        var events = new ArrayList<AuditEventView>();
        auditService.forEach(query, events::add);
        return events;
    }

    private List<String> actors(AuditQuery query) {
        return events(query).stream().map(AuditEventView::actor).toList();
    }

    private static AuditEventView only(List<AuditEventView> events) {
        assertEquals(1, events.size());
        return events.getFirst();
    }

    private void save(Instant time,
                      String actor,
                      AuditAction action,
                      AuditOutcome outcome,
                      String target,
                      String subject) {
        auditRecordDao.save(newRecord(time, actor, action, outcome, target, subject));
    }

    private static AuditRecord newRecord(Instant time,
                                         String actor,
                                         AuditAction action,
                                         AuditOutcome outcome,
                                         String target,
                                         String subject) {
        var auditRecord = new AuditRecord();
        auditRecord.setEventTime(time);
        auditRecord.setActor(actor);
        auditRecord.setCategory(action.getCategory());
        auditRecord.setAction(action);
        auditRecord.setOutcome(outcome);
        if (target != null) {
            auditRecord.setTargetType(AuditTargetType.PROJECT);
            auditRecord.setTarget(target);
        }
        if (subject != null) {
            auditRecord.setSubjectType(AuditSubjectType.GROUP);
            auditRecord.setSubject(subject);
        }
        return auditRecord;
    }
}
