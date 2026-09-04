package org.openl.studio.projects.messaging;

import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Names the clients whose requests are behind the change being published right now.
 *
 * <p>Every request that changes something is named. A browser tab names itself, putting an id of its
 * own in the {@value #ORIGIN_HEADER} header; a client that names none — an integration, an MCP
 * server, the legacy pages — is named after its HTTP session instead, and a request without even a
 * session gets an id of its own. The name rides the change ping back out, so the tab that made the
 * change recognises the echo of its own action, and a change made by anyone else never looks like
 * one.
 *
 * <p>Not every change is published by the request that made it. The workspace files watcher sees a
 * write from its own thread, and the design-repository index republishes from its executor a moment
 * after the commit that invalidated it — neither has a request to read a name from. A ping about one
 * user's workspace then takes the clients <em>of that user</em> that were writing just before: a
 * request holds its name for {@value #RECENT_MS} ms after it starts, which covers the disk events of
 * that request and the rebuild it triggers.
 *
 * <p>The fallback never crosses users, and stays conservative within one. Several clients of a user
 * writing within the window put all their names on the ping, so none of them reads it as its own
 * echo and every one re-reads. No recent writer at all leaves the ping unnamed, and nobody skips it.
 * A change that concerns every user — a commit reaching the design repository — is never named this
 * way at all: what one user was doing says nothing about a repository everyone shares.
 */
@Component
@RequiredArgsConstructor
public class ChangeOriginResolver {

    /** The header a client puts its own id in. */
    public static final String ORIGIN_HEADER = "X-OpenL-Client-Id";

    /** Where a client that named none keeps the id given to its session, or to its one request. */
    private static final String GIVEN_ORIGIN = ChangeOriginResolver.class.getName() + ".origin";

    /**
     * How long a client stays the presumed author of a change of its user's workspace that nothing
     * else can attribute. Long enough for the workspace disk events of its request and for the index
     * rebuild the request triggers; short enough that an unrelated change rarely falls inside it.
     */
    private static final long RECENT_MS = 5_000;

    /**
     * An id is an opaque token the client picks for itself; anything longer or built of other
     * characters is ignored, so nothing unbounded reaches the subscribers of a ping.
     */
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_.-]{1,64}");

    /**
     * How many recent writers are kept before the expired ones are swept out. A sweep also happens
     * whenever a change has to be attributed, which is after almost every write; the bound is what
     * keeps a run of writes that nothing looks at from growing the map without end.
     */
    private static final int SWEEP_ABOVE = 64;

    /** When each client last started changing something, by the user it acted for. */
    private final Map<Writer, Long> writingSince = new ConcurrentHashMap<>();

    private final Clock clock;

    /** A client of one user: the pair a workspace change can be attributed to. */
    private record Writer(String userName, String origin) {
    }

    /**
     * The clients to name on a change of the given user's workspace.
     *
     * <p>The request making the change, when there is one; otherwise the clients of that user that
     * were writing within the last few seconds. Empty when nothing can be attributed.
     *
     * @param userName the user whose workspace changed
     */
    public Set<String> origins(String userName) {
        var current = current();
        return current == null ? recentOf(userName) : Set.of(current);
    }

    /**
     * Who to name on a change of the design repository, by the user the ping goes to.
     *
     * <p>That ping reaches every user, so it is sent to each of them separately and each is told
     * only about clients of their own: a name is never carried to somebody who could not have set
     * it. A user who was not writing is named nothing and re-reads, as they must.
     */
    public Map<String, Set<String>> recentWritersByUser() {
        sweepExpired();
        var current = current();
        var currentUser = currentUserName();
        if (current != null && currentUser != null) {
            return Map.of(currentUser, Set.of(current));
        }
        return writingSince.keySet()
                .stream()
                .collect(Collectors.groupingBy(Writer::userName,
                        Collectors.mapping(Writer::origin, Collectors.toUnmodifiableSet())));
    }

    /**
     * Remembers that this client is changing something, so a publication that arrives without a
     * request of its own can still name it. A request made by nobody in particular is not
     * remembered: its change belongs to no user's workspace.
     *
     * @param request the request making the change
     */
    public void remember(HttpServletRequest request) {
        var userName = currentUserName();
        if (userName == null) {
            return;
        }
        if (writingSince.size() > SWEEP_ABOVE) {
            sweepExpired();
        }
        writingSince.put(new Writer(userName, originOf(request)), clock.millis());
    }

    /**
     * The name of the client whose request runs on this thread.
     *
     * @return the name, or {@code null} when no request is making the change
     */
    @Nullable
    public String current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return originOf(attributes.getRequest());
        }
        return null;
    }

    /**
     * What names this request's client: the id it sent, the id given to its session, or one given
     * to this request alone. A given id is kept where it was given, so every look-up within one
     * request answers the same. Given ids can never be the id of a browser tab, so a change made
     * through another client is never read as a tab's own echo.
     */
    private String originOf(HttpServletRequest request) {
        var sent = request.getHeader(ORIGIN_HEADER);
        if (sent != null && ID.matcher(sent).matches()) {
            return sent;
        }
        if (request.getAttribute(GIVEN_ORIGIN) instanceof String given) {
            return given;
        }
        var session = request.getSession(false);
        if (session != null && session.getAttribute(GIVEN_ORIGIN) instanceof String known) {
            return known;
        }
        // An id of its own, never the session id: the name reaches the other sessions of the user.
        // Kept on the request, so every look-up within it answers the same, and on the session when
        // there is one, so a client that keeps its cookie keeps one name across its calls.
        var created = UUID.randomUUID().toString();
        request.setAttribute(GIVEN_ORIGIN, created);
        if (session != null) {
            session.setAttribute(GIVEN_ORIGIN, created);
        }
        return created;
    }

    @Nullable
    private static String currentUserName() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || !authentication.isAuthenticated() ? null : authentication.getName();
    }

    /** The clients of the user that started changing something within the last few seconds. */
    private Set<String> recentOf(String userName) {
        sweepExpired();
        return writingSince.keySet()
                .stream()
                .filter(writer -> writer.userName().equals(userName))
                .map(Writer::origin)
                .collect(Collectors.toUnmodifiableSet());
    }

    private void sweepExpired() {
        var oldest = clock.millis() - RECENT_MS;
        writingSince.values().removeIf(since -> since < oldest);
    }
}
