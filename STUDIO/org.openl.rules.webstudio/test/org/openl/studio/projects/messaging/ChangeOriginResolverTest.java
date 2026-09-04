package org.openl.studio.projects.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ChangeOriginResolverTest {

    /** A clock the test moves by hand, so the recent-writer window needs no waiting. */
    private static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-08-12T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private final TestClock clock = new TestClock();
    private final ChangeOriginResolver resolver = new ChangeOriginResolver(clock);

    private static final String USER = "jane";

    @BeforeEach
    void signIn() {
        actingAs(USER);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    private static void actingAs(String userName) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(userName, "", List.of()));
    }

    private MockHttpServletRequest request() {
        var request = new MockHttpServletRequest("POST", "/web/projects/p1/files/a.txt");
        request.setSession(new MockHttpSession());
        return request;
    }

    private MockHttpServletRequest namedRequest(String origin) {
        var request = request();
        request.addHeader(ChangeOriginResolver.ORIGIN_HEADER, origin);
        return request;
    }

    private void running(MockHttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private void wrote(MockHttpServletRequest request) {
        resolver.remember(request);
    }

    private Set<String> origins() {
        return resolver.origins(USER);
    }

    @Test
    void names_the_client_the_request_came_from() {
        running(namedRequest("9f1c3b7e-0f5d-4b1a-9a7c-2f0d5c8e6b41"));

        assertEquals("9f1c3b7e-0f5d-4b1a-9a7c-2f0d5c8e6b41", resolver.current());
        assertEquals(Set.of("9f1c3b7e-0f5d-4b1a-9a7c-2f0d5c8e6b41"), origins());
    }

    @Test
    void a_client_that_names_none_is_named_after_its_session() {
        // An integration or an MCP server calling the REST API under the user's own credentials.
        var integration = request();
        running(integration);
        var first = resolver.current();

        // The same session keeps the same name, so a burst of its calls names one client.
        running(integration);
        assertEquals(first, resolver.current());

        // And it is never the id a browser tab of the same user gave itself, so that tab reads the
        // change as somebody else's and re-reads.
        assertNotEquals("9f1c3b7e-0f5d-4b1a-9a7c-2f0d5c8e6b41", first);
        assertTrue(first != null && !first.isBlank());
    }

    @Test
    void two_clients_of_one_user_are_told_apart_by_their_sessions() {
        running(request());
        var integration = resolver.current();
        running(request());

        assertNotEquals(integration, resolver.current());
    }

    @Test
    void a_change_made_outside_a_request_has_no_name_of_its_own() {
        // The files watcher and the repository index publish from their own threads.
        assertNull(resolver.current());
    }

    @Test
    void an_id_that_is_not_an_opaque_token_falls_back_to_the_session() {
        // Nothing unbounded or unexpected reaches the subscribers of a ping.
        var request = namedRequest("a".repeat(65));
        running(request);
        var fallback = resolver.current();

        assertNotEquals("a".repeat(65), fallback);
        assertTrue(fallback != null && fallback.length() <= 64);
    }

    @Test
    void a_change_published_without_a_request_names_the_client_that_was_just_writing() {
        // The disk event of a file the request wrote, or the index rebuild the commit triggered.
        wrote(namedRequest("tab-1"));

        clock.advance(Duration.ofSeconds(2));

        assertEquals(Set.of("tab-1"), origins());
    }

    @Test
    void two_clients_writing_at_once_leave_the_change_attributed_to_both() {
        // Neither may read the ping as its own echo: the other's change hides behind it.
        wrote(namedRequest("tab-1"));
        wrote(namedRequest("tab-2"));

        assertEquals(Set.of("tab-1", "tab-2"), origins());
    }

    @Test
    void a_tab_does_not_take_the_change_of_an_integration_for_its_own_echo() {
        // The user acts in a browser tab, and a moment later an integration of theirs writes too.
        wrote(namedRequest("tab-1"));
        var integration = request();
        wrote(integration);

        var origins = origins();

        assertEquals(2, origins.size(), "Both writers are named, so neither skips the other's change.");
        assertTrue(origins.contains("tab-1"));
    }

    @Test
    void what_one_user_was_writing_never_names_the_change_of_another() {
        // Their workspaces are separate, and a name borrowed across them would let one user skip a
        // change of the other as if it were their own echo.
        wrote(namedRequest("tab-1"));

        actingAs("john");

        assertEquals(Set.of(), resolver.origins("john"));
    }

    @Test
    void a_change_everyone_can_see_names_each_user_their_own_clients() {
        // The design repository is shared, so its ping goes to every user - each told only about
        // clients of their own, so a name never reaches somebody who could not have set it.
        wrote(namedRequest("tab-1"));
        actingAs("john");
        wrote(namedRequest("tab-2"));

        assertEquals(Map.of(USER, Set.of("tab-1"), "john", Set.of("tab-2")), resolver.recentWritersByUser());
    }

    @Test
    void the_request_making_a_change_everyone_can_see_names_its_user_alone() {
        wrote(namedRequest("tab-1"));
        actingAs("john");
        running(namedRequest("tab-2"));

        assertEquals(Map.of("john", Set.of("tab-2")), resolver.recentWritersByUser());
    }

    @Test
    void a_request_made_by_nobody_is_not_remembered() {
        SecurityContextHolder.clearContext();

        wrote(namedRequest("tab-1"));

        actingAs(USER);
        assertEquals(Set.of(), origins());
    }

    @Test
    void a_writer_stops_being_the_presumed_author_once_the_window_passes() {
        wrote(namedRequest("tab-1"));

        clock.advance(Duration.ofSeconds(6));

        // An external push arriving long after nobody's own action belongs to nobody.
        assertEquals(Set.of(), origins());
    }

    @Test
    void the_request_making_the_change_outranks_whoever_wrote_before_it() {
        wrote(namedRequest("tab-1"));
        running(namedRequest("tab-2"));

        assertEquals(Set.of("tab-2"), origins());
    }
}
